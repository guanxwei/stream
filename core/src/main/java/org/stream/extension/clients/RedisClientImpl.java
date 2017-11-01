package org.stream.extension.clients;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stream.core.exception.RedisException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Tuple;

/**
 * Default implementation of {@linkplain RedisClient}
 */
@Setter @Getter
@AllArgsConstructor
public class RedisClientImpl implements RedisClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisClientImpl.class);
    private static final Logger ERROR = LoggerFactory.getLogger(RedisClientImpl.class);
    private static final int DEFAULT_EXPIRE_TIME = 3600 * 24 * 30;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final String DELIMETER = ";";

    private String masters;
    private String sentinels;
    private String password;
    private int timeout = 5000;
    private int maxActive;
    private int maxIdle = 10;
    private int minIdle = 5;

    private AssembledShardedJedisSentinelPool jedisPool;

    /**
     * Default constructor.
     */
    public RedisClientImpl() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean set(final String key, final String value) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            String result = sJedis.getMasterShardedRedis().setex(key, DEFAULT_EXPIRE_TIME, value);
            boolean isSucc = "OK".equalsIgnoreCase(result);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("set " + key + "->" + value + "," + isSucc);
            }
            jedisPool.returnResource(sJedis);
            return isSucc;
        } catch (Exception e) {
            ERROR.error("set error :" + key + ", " + value, e);
            jedisPool.returnBrokenResource(sJedis);
            throw new RedisException("set reid failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long sadd(final String key, final String value) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            Long result = sJedis.getMasterShardedRedis().getShard(key).sadd(key, value);
            this.jedisPool.returnResource(sJedis);
            return result;
        } catch (Exception e) {
            this.jedisPool.returnBrokenResource(sJedis);
            ERROR.error("sadd error,key:" + key);
            throw new RedisException("lpop key failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean del(final String key) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            long count = sJedis.getMasterShardedRedis().del(key);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("del " + key + " -> " + count);
            }
            jedisPool.returnResource(sJedis);
            return true;
        } catch (Exception e) {
            ERROR.error("del error :" + key, e);
            jedisPool.returnBrokenResource(sJedis);
            throw new RedisException("delete key failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long sremove(final String setKey, final String... members) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            long v = sJedis.getMasterShardedRedis().srem(setKey, members);
            this.jedisPool.returnResource(sJedis);
            return v;
        } catch (Exception e) {
            this.jedisPool.returnBrokenResource(sJedis);
            ERROR.error("sremove error,key:" + setKey + " members:" + StringUtils.join(members));
            throw new RedisException("sremove failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String get(final String key) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            String result = sJedis.getMasterShardedRedis().get(key);
            if (LOGGER.isDebugEnabled()) {

                LOGGER.debug("master get " + key + " -> " + result);
            }
            jedisPool.returnResource(sJedis);
            return result;
        } catch (Exception e) {
            // 主读不了,尝试读从;
            try {
                if (sJedis.isSlaveUseable()) {
                    String result = sJedis.getSlaveShardedRedis().get(key);
                    if (LOGGER.isDebugEnabled()) {

                        LOGGER.debug("slave get " + key + " -> " + result);
                    }
                    jedisPool.returnResource(sJedis);
                    return result;
                } else {
                    jedisPool.returnBrokenResource(sJedis);
                    ERROR.error("get error :" + key, e);
                    throw new RedisException("get jedis failed!", e);
                }
            } catch (Exception ex) {
                jedisPool.returnBrokenResource(sJedis);
                ERROR.error("get error :" + key, ex);
                throw new RedisException("get jedis failed!", ex);
            }
        }
    }

    /**
     * Initiate method.
     */
    public void init() {
        List<String> masterList = Arrays.asList(StringUtils.split(masters, DELIMETER));
        Set<String> sentinelSet = new HashSet<String>(Arrays.asList(StringUtils.split(sentinels, DELIMETER)));
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTestOnBorrow(false);
        config.setTimeBetweenEvictionRunsMillis(20000); // 10 seconds.
        config.setSoftMinEvictableIdleTimeMillis(30000);
        config.setNumTestsPerEvictionRun(maxActive);
        config.setTestWhileIdle(true);
        if ("null".equalsIgnoreCase(password)) {
            jedisPool = new AssembledShardedJedisSentinelPool(masterList, sentinelSet, config, timeout);
        } else {
            jedisPool = new AssembledShardedJedisSentinelPool(masterList, sentinelSet, config, timeout, password);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AssembledShardedJedis getJedis() {
        try {
            return jedisPool.getResource();
        } catch (Exception e) {
            LOGGER.error("can't get client from jedis pool:" + this.masters, e);
            throw new RedisException("get jedis failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> lrange(final String key, final long start, final long end) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            List<String> result = sJedis.getMasterShardedRedis().getShard(key).lrange(key, start, end);
            this.jedisPool.returnResource(sJedis);
            return result;
        } catch (Exception e) {
            ERROR.error("lrange error,key:" + key, e);
            this.jedisPool.returnBrokenResource(sJedis);
            throw new RedisException("lrange key failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long setnx(final String key, final String value) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            Long code = sJedis.getMasterShardedRedis().setnx(key, value);
            jedisPool.returnResource(sJedis);
            return code;
        } catch (Exception e) {
            ERROR.error("set error :" + key + ", " + value, e);
            jedisPool.returnBrokenResource(sJedis);
            throw new RedisException("set reid failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long lpush(final String key, final String... values) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            Long length = sJedis.getMasterShardedRedis().lpush(key, values);
            jedisPool.returnResource(sJedis);
            return length;
        } catch (Exception e) {
            this.jedisPool.returnBrokenResource(sJedis);
            ERROR.error("lpush error,key:" + key + ",value:" + Arrays.toString(values), e);
            throw new RedisException("lpush key failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean lrem(final String key, final int count, final String value) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            boolean b = sJedis.getMasterShardedRedis().lrem(key, count, value) >= 0;
            this.jedisPool.returnResource(sJedis);
            return b;
        } catch (Exception e) {
            ERROR.error("zremrangebyrank error,key:" + key, e);
            this.jedisPool.returnBrokenResource(sJedis);
            throw new RedisException("lrem key failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setWithExpireTime(final String key, final String value, final int seconds) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            String result = sJedis.getMasterShardedRedis().setex(key, seconds, value);
            boolean isSucc = "OK".equalsIgnoreCase(result);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("set " + key + "->" + value + "," + seconds + "," + isSucc);
            }
            jedisPool.returnResource(sJedis);
            return isSucc;
        } catch (Exception e) {
            ERROR.error("set error :" + key + ", " + value + ", " + seconds, e);
            jedisPool.returnBrokenResource(sJedis);
            throw new RedisException("expire key failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long zadd(final String key, final String value, final double score) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            Long result = sJedis.getMasterShardedRedis().zadd(key, score, value);
            jedisPool.returnResource(sJedis);
            return result;
        } catch (Exception e) {
            ERROR.error("zadd error :" + key + ", " + value + ", " + score, e);
            jedisPool.returnBrokenResource(sJedis);
            throw new RedisException("zadd error", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Tuple> zrange(final String key, final int begin, final int end) {
        AssembledShardedJedis sJedis = getJedis();
        try {
            Set<Tuple> result = sJedis.getMasterShardedRedis().getShard(key).zrangeWithScores(key, begin, end);
            this.jedisPool.returnResource(sJedis);
            return result;
        } catch (Exception e) {
            ERROR.error("zrange error,key:" + key, e);
            this.jedisPool.returnBrokenResource(sJedis);
            throw new RedisException("lrange key failed!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void returnResource(final AssembledShardedJedis jedis) {
        this.jedisPool.returnResource(jedis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void returnBrokenResource(final AssembledShardedJedis jedis) {
        this.jedisPool.returnBrokenResource(jedis);
    }
}

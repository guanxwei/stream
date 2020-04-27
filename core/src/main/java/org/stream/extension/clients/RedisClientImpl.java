package org.stream.extension.clients;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.stream.extension.persist.TaskPersisterImpl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

/**
 * Default implementation of {@linkplain RedisClient}
 */
@Slf4j
@Setter @Getter
public class RedisClientImpl implements RedisClient {

    private JedisCluster jedisCluster;

    private String nodes;

    private int timeout;

    private int maxRetryTimes;

    private String passWord;

    private int maxActive;

    private int maxIdle;

    private int minIdle;

    /**
     * Default constructor.
     */
    public RedisClientImpl() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean set(final String key, final String value) {
        return jedisCluster.set(key, value).equals("OK");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long sadd(final String key, final String value) {
        return jedisCluster.sadd(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean del(final String key) {
        return jedisCluster.del(key) >= 1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long sremove(final String setKey, final String... members) {
        return jedisCluster.srem(setKey, members);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String get(final String key) {
       return jedisCluster.get(key);
    }

    /**
     * Initiate method.
     */
    public void init() {
        if (nodes != null) {
            initiateRedisCluster();
            return;
        }
        throw new RuntimeException("Cluster nodes not specified");
    }

    private void initiateRedisCluster() { 
        String[] hosts = nodes.split(";");
        Set<HostAndPort> hostAndPorts = new HashSet<>();
        for (String host : hosts) {
            String[] pair = host.split(":");
            HostAndPort hostAndPort = new HostAndPort(pair[0], Integer.parseInt(pair[1]));
            hostAndPorts.add(hostAndPort);
        }
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxTotal(maxActive);
        config.setMaxWaitMillis(500);
        jedisCluster = new JedisCluster(hostAndPorts, timeout, maxRetryTimes, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> lrange(final String key, final long start, final long end) {
        return jedisCluster.lrange(key, start, end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long setnx(final String key, final String value) {
        String result = jedisCluster.set(key, value,
                SetParams.setParams().nx().ex(TaskPersisterImpl.LOCK_EXPIRE_TIME / 1000));
        log.info("lock result [{}]", result);
        return "OK".equalsIgnoreCase(result) ? 1l : 0l;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long lpush(final String key, final String... values) {
        return jedisCluster.lpush(key, values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean lrem(final String key, final int count, final String value) {
        return jedisCluster.lrem(key, count, value) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setWithExpireTime(final String key, final String value, final int seconds) {
        return jedisCluster.setex(key, seconds, value).equals("OK");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long zadd(final String key, final String value, final double score) {
        return jedisCluster.zadd(key, score, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> zrange(final String key, final double begin, final double end) {
        return jedisCluster.zrangeByScore(key, begin, end, 0, 10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean zdel(final String set, final String key) {
        return jedisCluster.zrem(set, key) >= 1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean rpush(final String set, final String key) {
        return jedisCluster.rpush(set, key) == 1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getListSize(final String list) {
        return jedisCluster.llen(list);
    }
}

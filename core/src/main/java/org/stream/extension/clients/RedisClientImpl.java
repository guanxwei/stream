/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stream.extension.clients;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.stream.extension.settings.Settings;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

/**
 * Default implementation of {@linkplain RedisClient}
 */
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
    public Long setnxWithExpireTime(final String key, final String value) {
        return "ok".equalsIgnoreCase(jedisCluster
                .set(key, value, SetParams.setParams().nx().px(Settings.LOCK_EXPIRE_TIME))) ? 1l : 0l;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateKeyExpireTimeIfMatch(final String key, final String expectedValue) {
        if (Settings.LUA_SUPPORTED) {
            Long response = (Long) jedisCluster.eval(buildLuaScript(key, expectedValue), List.of(key, expectedValue), List.of());
            return response == 1L;
        } else {
            String value = jedisCluster.get(key);
            if (expectedValue.equals(value)) {
                return jedisCluster.expire(key, Settings.LOCK_EXPIRE_TIME / 1000) == 1;
            }
            return false;
        }
    }

    private String buildLuaScript(final String key, final String expectedValue) {
        if (StringUtils.isNotBlank(Settings.UPDATE_EXPIRE_TIME_LUA_SCRIPT)) {
            return Settings.UPDATE_EXPIRE_TIME_LUA_SCRIPT;
        }
        return String.format("local key = KEYS[1];\n"
                         + "local value = redis.call('GET', key);\n"
                         + "local result = 0;\n"
                         + "if KEYS[2] == value\n"
                         + "then\n"
                         + "result = redis.call('EXPIRE', key, %d);\n"
                         + "end;\n"
                         + "return result;", Settings.LOCK_EXPIRE_TIME / 1000);
    }
    /**
        test output of the lua script.
            local key = KEYS[1];
            local value = redis.call('GET', key);
            local result = 0;
            if KEYS[2] == value
            then
            result = redis.call('EXPIRE', key, 6);
            end;
            return result;
    **/
}

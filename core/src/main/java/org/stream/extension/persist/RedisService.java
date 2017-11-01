package org.stream.extension.persist;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.stream.core.exception.RedisException;
import org.stream.extension.clients.AssembledShardedJedis;
import org.stream.extension.clients.AssembledShardedJedisSentinelPool;
import org.stream.extension.clients.RedisClientImpl;

/**
 * Encapsulation of Redis Service.
 * Provide some valuable APIs to manufacture Redis DB.
 * @author hzweiguanxiong
 *
 */
public class RedisService extends RedisClientImpl {

    /**
     * Initiation method to initiate Redis clients pool.
     * @param masters Master list.
     * @param sentinels Redis sentinels.
     * @param password Password.
     * @param timeout Timeout.
     * @param maxActive Max active clients.
     */
    public RedisService(final String masters, final String sentinels, final String password,
            final int timeout, final int maxActive) {
        super();
        super.setMasters(masters);
        super.setTimeout(timeout);
        super.setSentinels(sentinels);
        super.setMaxActive(maxActive);
        super.init();
    }

    /**
     * Add a new record in Redis.
     * @param key Record's key.
     * @param value Record's value.
     * @return Manipulation result.
     */
    public boolean set(final String key, final String value) {
        super.set(key, value);
        return true;
    }

    /**
     * Add a new record in the specific Set.
     * @param setName Set's name
     * @param key New added key.
     * @return Manipulation result.
     */
    public Long addKey(final String setName, final String key) {
        return super.sadd(setName, key);
    }

    /**
     * Get content list from the specific Set.
     * @param setName The Set's name.
     * @param limit Up-limitation.
     * @return Content list.
     */
    public List<String> getList(final String setName, final int limit) {
        Method method;
        AssembledShardedJedisSentinelPool jedisPool = null;
        AssembledShardedJedis sJedis = null;
        try {
            method = this.getClass().getSuperclass().getDeclaredMethod("getJedis",  new Class<?>[0]);
            Field filed = this.getClass().getSuperclass().getDeclaredField("jedisPool");
            method.setAccessible(true);
            filed.setAccessible(true);
            jedisPool = (AssembledShardedJedisSentinelPool) filed.get(this);
            sJedis = (AssembledShardedJedis) method.invoke(this, new Object[]{});
            List<String> result = sJedis.getSlaveShardedRedis().srandmember(setName, limit);
            jedisPool.returnResourceObject(sJedis);
            return result;
        } catch (Exception e) {
            if (jedisPool != null) {
                jedisPool.returnBrokenResource(sJedis);
            }
            throw new RedisException("zrank key failed!", e);
        }
    }

    /**
     * Delete an record from Redis DB.
     * @param key Key to be deleted.
     * @return Manipulation result.
     */
    public boolean deleteKey(final String key) {
        super.del(key);
        return true;
    }

    /**
     * Delete an record in the specific Set.
     * @param setName The set's name.
     * @param value The value to be deleted.
     * @return Manipulation result.
     */
    public boolean deleteValueFromSet(final String setName, final String value) {
        super.sremove(setName, value);
        return true;
    }

    /**
     * Get an record via key.
     * @param key Record's key.
     * @return Record's value.
     */
    public String get(final String key) {
        return super.get(key);
    }
}

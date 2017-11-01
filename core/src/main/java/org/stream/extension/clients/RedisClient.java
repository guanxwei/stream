package org.stream.extension.clients;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.Tuple;

/**
 * Redis service client.
 * Only define partial functions that Redis provides.
 * @author 魏冠雄
 *
 */
public interface RedisClient {

    /**
     * Add one a new key-value pair in Redis if the key does not exists, otherwise update the value.
     * @param key Key.
     * @param value Value to be updated.
     * @return Manipulation reuslt.
     */
    boolean set(final String key, final String value);

    /**
     * Add a new value in the specific Set.
     * @param key Set name.
     * @param value Value to be set.
     * @return Affected rows.
     */
    Long sadd(final String key, final String value);

    /**
     * Delete the key and its value.
     * @param key Key to be deleted.
     * @return Manipulation result.
     */
    boolean del(final String key);

    /**
     * Remove a bundle of members from the Set.
     * @param setKey Set name.
     * @param memebers Members to be removed.
     * @return Affected rows.
     */
    long sremove(final String setKey, final String... memebers);

    /**
     * Get value from the Redis with key.
     * @param key Key name.
     * @return Value.
     */
    String get(final String key);

    /**
     * Get partial the list from the Redis.
     * @param key List name.
     * @param start Start cursor.
     * @param end End cursor.
     * @return Partial the list.
     */
    List<String> lrange(final String key, final long start, final long end);

    /**
     * Set the key only and if only the key did not exist.
     * @param key Key to be set.
     * @param value Value attached to the key.
     * @return Affected rows.
     */
    Long setnx(final String key, final String value);

    /**
     * Save a bundle of values in the specific list.
     * @param key List name.
     * @param value Value list.
     * @return Affected rows.
     */
    Long lpush(final String key, final String... value);

    /**
     * Remove values from the list. Detail please refer to <a>https://redis.io/commands/lrem</a>
     * @param key List name.
     * @param count See <a>https://redis.io/commands/lrem</a>.
     * @param value Value to be removed.
     * @return Manipulation result.
     */
    boolean lrem(final String key, final int count, final String value);

    /**
     * Set the new key with expire time.
     * @param key Key name.
     * @param value Value.
     * @param seconds Existing time.
     * @return Manipulation result.
     */
    boolean setWithExpireTime(final String key, final String value, final int seconds);

    /**
     * Add a new value in the specific sorted Set.
     * @param key Set name.
     * @param value Value to be set.
     * @param score Score.
     * @return Affected rows.
     */
    Long zadd(final String key, final String value, final double score);

    /**
     * Get a sub-set from the sorted sort.
     * @param key Sorted set.
     * @param begin Begin cursor.
     * @param end End cursor.
     * @return Result.
     */
    Set<Tuple> zrange(final String key, final int begin, final int end);

    /**
     * Get Redis connection.
     * @return Redis connection.
     */
    AssembledShardedJedis getJedis();

    /**
     * Return resource.
     * @param jedis Resource to be returned.
     */
    void returnResource(final AssembledShardedJedis jedis);

    /**
     * Return broken resource.
     * @param jedis Resource to be returned.
     */
    void returnBrokenResource(final AssembledShardedJedis jedis);
}

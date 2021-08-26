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

import java.util.List;
import java.util.Set;

import org.stream.extension.settings.Settings;

/**
 * Redis service client.
 * Only define partial functions that Redis provides. This client should only be used by the Stream framework itself.
 * @author weiguanxiong
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
     * Set the key only and if only the key did not exist. Expire time will be set as {@link Settings#LOCK_EXPIRE_TIME},
     * customers are not allowed to specify the expire time themselves.
     * @param value Value attached to the key.
     * @param key Redis key.
     * @return Affected rows.
     */
    Long setnxWithExpireTime(final String key, final String value);

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
    Set<String> zrange(final String key, final double begin, final double end);

    /**
     * Execute zdel commmand.
     * @param set Set name.
     * @param key Key to be deleted.
     * @return {@code true} deleted, {@code false} not deleted.
     */
    boolean zdel(final String set, final String key);

    /**
     * Execute rpush commmand.
     * @param set Set name.
     * @param key Key to be added.
     * @return Affected rows.
     */
    boolean rpush(final String set, final String key);

    /**
     * Get the list size.
     * @param list Target list.
     * @return List size.
     */
    long getListSize(final String list);

    /**
     * Update the specific key's expire time if exits and the stored value matches the expected one.
     * Add more {@link Settings#LOCK_EXPIRE_TIME} milliseconds.
     * @param key Redis key.
     * @param expectedValue Expected value of the specific key.
     * @return <code>true</code> If condition fulfills and operation succeeds otherwise {@code false}
     */
    boolean updateKeyExpireTimeIfMatch(final String key, final String expectedValue);
}

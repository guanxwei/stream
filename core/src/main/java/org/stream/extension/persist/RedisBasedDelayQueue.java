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

package org.stream.extension.persist;

import java.util.Collection;

import org.stream.extension.clients.RedisClient;

import lombok.Setter;

/**
 * A redis based implementation of distributed delay queue.
 * @author guanxiong wei
 *
 */
public class RedisBasedDelayQueue implements DelayQueue {

    @Setter
    private RedisClient redisClient;

    /**
     * {@inheritDoc}
     */
    public Collection<String> getItems(final String queueName, final double end) {
        return redisClient.zrange(queueName, 0d, end);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteItem(final String queueName, final String item) {
        redisClient.zdel(queueName, item);
    }

    /**
     * {@inheritDoc}
     */
    public void enqueue(final String queueName, final String item, final double delayTime) {
        redisClient.zadd(queueName, item, delayTime);
    }
}

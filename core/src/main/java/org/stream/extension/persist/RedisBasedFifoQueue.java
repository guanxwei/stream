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
 * A first in first out queue.
 * Duplicated item is not allowed, if the new pushed item exists in the queue, will remove it first
 * then put it at the tail of the queue.
 * @author weiguanxiong.
 *
 */
public class RedisBasedFifoQueue implements FifoQueue {

    @Setter
    private RedisClient redisClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public void push(final String queueName, final String item) {
        redisClient.lrem(queueName, 0, item);
        // Add the task id at the tail of the back up queue.
        redisClient.rpush(queueName, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final String queueName, final String item) {
        return redisClient.lrem(queueName, 0, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> pop(final String queueName, final int end) {
        return redisClient.lrange(queueName, 0, end);
    }
}

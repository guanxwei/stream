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

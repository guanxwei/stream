package org.stream.extension.persist;

import java.util.Collection;

import lombok.Setter;

/**
 * A first in first out queue.
 * Duplicated item is not allowed, if the new pushed item exists in the queue, will remove it first
 * then put it at the tail of the queue.
 * @author weiguanxiong.
 *
 */
public class FifoQueue {

    @Setter
    private RedisService redisService;

    /**
     * Push the item to the tail of the queue.
     * @param queueName Target queue.
     * @param item Item to be added
     */
    public void push(final String queueName, final String item) {
        redisService.lrem(queueName, 0, item);
        // Add the task id at the tail of the back up queue.
        redisService.rpush(queueName, item);
    }

    /**
     * Remove the item from the queue.
     * @param queueName Queue name.
     * @param item Target item.
     * @return <code>true</code> item removed, otherwise <code>false</code>
     */
    public boolean remove(final String queueName, final String item) {
        return redisService.lrem(queueName, 0, item);
    }

    /**
     * Get top items.
     * @param queueName Delay queue name.
     * @param end End index.
     * @return Delayed items.
     */
    public Collection<String> pop(final String queueName, final int end) {
        return redisService.lrange(queueName, 0, end);
    }
}

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

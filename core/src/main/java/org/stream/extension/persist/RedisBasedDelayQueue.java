package org.stream.extension.persist;

import java.util.Collection;

import lombok.Setter;

/**
 * A redis based implementation of distributed delay queue.
 * @author weigu
 *
 */
public class RedisBasedDelayQueue implements DelayQueue {

    @Setter
    private RedisService redisService;

    /**
     * {@inheritDoc}
     */
    public Collection<String> getItems(final String queueName, final double end) {
        return redisService.zrange(queueName, 0d, end);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteItem(final String queueName, final String item) {
        redisService.zdel(queueName, item);
    }

    /**
     * {@inheritDoc}
     */
    public void enqueue(final String queueName, final String item, final double delayTime) {
        redisService.zadd(queueName, item, delayTime);
    }
}

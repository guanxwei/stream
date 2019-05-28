package org.stream.extension.persist;

import java.util.Collection;

import lombok.Setter;

/**
 * A redis based implementation of distributed delay queue.
 * @author weigu
 *
 */
public class DelayQueue {

    @Setter
    private RedisService redisService;

    /**
     * Get enqueued items.
     * @param queueName Delay queue name.
     * @param end End time.
     * @return Delayed items.
     */
    public Collection<String> getItems(final String queueName, final double end) {
        return redisService.zrange(queueName, 0d, end);
    }

    /**
     * Delete the enqueued item.
     * @param queueName Delay queue name.
     * @param item Target item to be deleted.
     */
    public void deleteItem(final String queueName, final String item) {
        redisService.zdel(queueName, item);
    }

    /**
     * Put an item into the delay queue.
     * @param queueName Delay queue name.
     * @param item Target item to be deleted.
     * @param delayTime delay time.
     */
    public void enqueue(final String queueName, final String item, final double delayTime) {
        redisService.zadd(queueName, item, delayTime);
    }
}

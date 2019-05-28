package org.stream.extension.persist;

import java.util.Collection;

/**
 * Delay queue.
 * @author weiguanxiong.
 *
 */
public interface DelayQueue {

    /**
     * Get enqueued items.
     * @param queueName Delay queue name.
     * @param end End time.
     * @return Delayed items.
     */
    Collection<String> getItems(final String queueName, final double end);

    /**
     * Delete the enqueued item.
     * @param queueName Delay queue name.
     * @param item Target item to be deleted.
     */
    void deleteItem(final String queueName, final String item);

    /**
     * Put an item into the delay queue.
     * @param queueName Delay queue name.
     * @param item Target item to be deleted.
     * @param delayTime delay time.
     */
    void enqueue(final String queueName, final String item, final double delayTime);
}

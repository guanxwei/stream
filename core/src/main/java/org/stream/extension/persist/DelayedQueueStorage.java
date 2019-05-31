package org.stream.extension.persist;

import java.util.Collection;

public interface DelayedQueueStorage {

    /**
     * Push the item to the tail of the queue.
     * @param queueName Target queue.
     * @param item Item to be added
     */
    public void push(final String queueName, final String item);

    /**
     * Remove the item from the queue.
     * @param queueName Queue name.
     * @param item Target item.
     * @return <code>true</code> item removed, otherwise <code>false</code>
     */
    public boolean remove(final String queueName, final String item);

    /**
     * Get top items.
     * @param queueName Delay queue name.
     * @param end End index.
     * @return Delayed items.
     */
    public Collection<String> pop(final String queueName, final int end);
}

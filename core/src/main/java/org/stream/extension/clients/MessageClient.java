package org.stream.extension.clients;

/**
 * Kafka client.
 *
 */
public interface MessageClient {

    /**
     * Send a message to the kafaka queue.
     *
     * @param topic The topic the record will be appended to
     * @param data  The record contents
     * @return Manipulation result.
     */
    public boolean sendMessage(final String topic, final byte[] data);

    /**
     * Send a message (with key) to the kafka queue.
     * @param topic The topic the record will be appended to
     * @param key  The key that will be included in the record
     * @param data The record contents
     * @return Manipulation result.
     */
    public boolean sendMessage(final String topic, final String key, final byte[] data);

    /**
     * Pull messages from the Kafaka queue according to the topic.
     * @param key Message topic.
     * @return Message queue head.
     */
    public byte[] pullMessage(final String key);

    /**
     * Utility method to atomically decrease the counter.
     * <p> Please make sure that the method {@link MessageClient#pullMessage(String)}} is invoked before invoking this method.
     * @return Manipulation result.
     */
    public boolean markAsConsumed();
}

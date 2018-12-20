package org.stream.extension.clients;

import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Kafka client.
 *
 */
public interface KafkaClient {

    /**
     * Send a message to the kafaka queue.
     *
     * @param topic The topic the record will be appended to
     * @param data  The record contents
     * @return Manipulation result.
     */
    public boolean sendMessage(final String topic, final String data);

    /**
     * Send a message (with key) to the kafka queue.
     * @param topic The topic the record will be appended to
     * @param key  The key that will be included in the record
     * @param data The record contents
     * @return Manipulation result.
     */
    public Future<RecordMetadata> sendMessage(final String topic, final String key, final String data);

    /**
     * Pull messages from the Kafaka queue according to the topic.
     * @param key Message topic.
     * @return Message queue head.
     */
    public String pullMessageAsString(final String key);

    /**
     * Utility method to atomically decrease the counter.
     * <p> Please make sure that the method {@link KafkaClient#pullMessageAsString(String)}} is invoked before invoking this method.
     * @return Manipulation result.
     */
    public boolean markAsConsumed();
}

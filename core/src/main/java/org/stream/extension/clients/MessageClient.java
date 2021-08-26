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

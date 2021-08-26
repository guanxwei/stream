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

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link MessageClient} implementation.
 * @author guanxiong wei
 *
 */
@Slf4j
public class KafkaClientImpl implements MessageClient {

    /**
     * Kafka uses ZooKeeper to coordinate with each blockers, so we need to set zk servers here.
     */
    @Setter @Getter(value = AccessLevel.PRIVATE)
    private String servers;
    @Setter @Getter(value = AccessLevel.PRIVATE)
    private List<String> keys;
    @Setter
    private String topic;
    @Setter
    private String group;

    private KafkaConsumer<String, byte[]> consumer;
    private KafkaProducer<String, byte[]> producer;
    private Properties consumerProperties;
    private Properties producerProperties;
    private ConcurrentHashMap<String, BlockingQueue<byte[]>> repository = new ConcurrentHashMap<>();
    private AtomicInteger accounter = new AtomicInteger();
    private ExecutorService service;

    /**
     * Initiate method to start up Kafka client.
     * This method should be invoked at the time the server is starting, in Spring managed context, it should
     * be marked as the initMethod of the defined bean.
     *
     * From functional perspective, this method will automatically load Kafka consumer and producer configuration and initiate the back-end
     * threads to help deliver message to the Kafka message queue and retrieve messages from the queue dispatching them to the right processors.
     */
    public void init() {
        initiateConfig();
        initiateKafkaStuffs();
        log.info("Kafka consumer and producer for topic [{}] started.", topic);

        service.submit(() -> {
            while (true) {
                try {
                    ConsumerRecords<String, byte[]> records = consumer.poll(3000);
                    for (ConsumerRecord<String, byte[]> record : records) {
                        log.info("Reveive kafka message from topic [{}] with key [{}]", topic, record.key());
                        repository.get(record.key()).offer(record.value());
                        accounter.incrementAndGet();
                    }
                    while (accounter.get() != 0) {
                        Thread.sleep(5);
                    }
                    consumer.commitSync();
                } catch (Exception e) {
                    log.warn("Something wrong happened during message pulling process", e);
                    consumer.close();
                    consumer = null;
                    initiateConsumer();
                }
            }
        });
    }

    private void initiateConfig() {
        consumerProperties = getDefaultConsumerProperties();
        producerProperties = getDefaultProduderProperties();
        consumerProperties.put("bootstrap.servers", getServers());
        producerProperties.put("bootstrap.servers", getServers());
        for (String key : getKeys()) {
            repository.put(key, new LinkedBlockingQueue<>(100));
        }
    }

    private void initiateKafkaStuffs() {
        producer = new KafkaProducer<>(producerProperties);
        service = Executors.newFixedThreadPool(keys.size());
        initiateConsumer();
    }

    private void initiateConsumer() {
        List<String> subscribe = new LinkedList<>();
        subscribe.add(topic);
        consumer = new KafkaConsumer<>(consumerProperties);
        consumer.subscribe(subscribe);
    }

    private Properties getDefaultProduderProperties() {
        Properties props = new Properties();
        props.put("acks", "all");
        props.put("retries", "1");
        props.put("batch.size", "16384");
        props.put("linger.ms", "10");
        props.put("buffer.memory", "33554432");
        props.put("group.id", group);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        return props;
    }

    private Properties getDefaultConsumerProperties() {
        Properties props = new Properties();
        props.put("enable.auto.commit", "false");
        props.put("request.timeout.ms", "150000");
        props.put("heartbeat.interval.ms", "10000");
        props.put("session.timeout.ms", "120000");
        props.put("max.poll.records", "100");
        props.put("group.id", group);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return props;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendMessage(final String topic, final byte[] data) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<String, byte[]>(topic, data);
        producer.send(record);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendMessage(final String topic, final String key, final byte[] data) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<String, byte[]>(topic, key, data);
        producer.send(record);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] pullMessage(final String key) {
        try {
            return repository.get(key).take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markAsConsumed() {
        accounter.decrementAndGet();
        return true;
    }

}

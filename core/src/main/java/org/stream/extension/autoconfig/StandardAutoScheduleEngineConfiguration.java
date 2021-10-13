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

package org.stream.extension.autoconfig;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.stream.core.component.ActivityRepository;
import org.stream.core.execution.AutoScheduledEngine;
import org.stream.core.execution.Engine;
import org.stream.core.execution.GraphContext;
import org.stream.core.helper.GraphLoader;
import org.stream.core.helper.LocalGraphLoader;
import org.stream.extension.clients.KafkaClientImpl;
import org.stream.extension.clients.MessageClient;
import org.stream.extension.clients.MongoClient;
import org.stream.extension.clients.MongoClientImpl;
import org.stream.extension.clients.RedisClient;
import org.stream.extension.events.EventCenter;
import org.stream.extension.events.Listener;
import org.stream.extension.events.MangoDBBasedTaskCompleteListener;
import org.stream.extension.events.MemoryEventCenter;
import org.stream.extension.events.TaskCompleteEvent;
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.executors.ThreadPoolTaskExecutor;
import org.stream.extension.lock.Lock;
import org.stream.extension.lock.providers.RedisClusterBasedLock;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.pattern.defaults.ScheduledTimeIntervalPattern;
import org.stream.extension.persist.KafkaBasedTaskStorage;
import org.stream.extension.persist.RedisService;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.persist.TaskPersisterImpl;
import org.stream.extension.persist.TaskStepStorage;
import org.stream.extension.persist.TaskStorage;
import org.stream.extension.utils.TaskIDGenerator;
import org.stream.extension.utils.UUIDTaskIDGenerator;

@Configuration
public class StandardAutoScheduleEngineConfiguration {

    @Resource
    private Environment environment;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private TaskStorage taskStorage;

    @Resource
    private TaskStepStorage taskStepStorage;

    @Bean
    public ActivityRepository activityRepository() {
        return new ActivityRepository();
    }

    @Bean
    public GraphContext graphContext() {
        GraphContext graphContext = new GraphContext();
        graphContext.setActivityRepository(activityRepository());
        return graphContext;
    }

    @Bean
    public GraphLoader graphLoader() {
        LocalGraphLoader localGraphLoader = new LocalGraphLoader();
        localGraphLoader.setGraphContext(graphContext());
        return localGraphLoader;
    }

    @Bean
    public AutoGraphLoader autoGraphLoader() {
        return new AutoGraphLoader();
    }

    @Bean
    public TaskPersister taskPersister() throws Exception {
        TaskPersisterImpl taskPersisterImpl = new TaskPersisterImpl();
        taskPersisterImpl.setApplication(environment.getProperty("application"));
        taskPersisterImpl.setLock(lock());
        taskPersisterImpl.setMessageQueueBasedTaskStorage(messageQueueBasedTaskStorage());
        taskPersisterImpl.setTaskStepStorage(taskStepStorage);
        taskPersisterImpl.setTaskStorage(taskStorage);

        return taskPersisterImpl;
    }

    @Bean
    public Lock lock() {
        return new RedisClusterBasedLock();
    }

    @Bean
    public RedisClient redisClient() {
        String redisNodes = environment.getProperty("fast.stream.redisclustuer.nodes");
        int timeout = environment.getProperty("fast.stream.redisclustuer.timetout", Integer.class);
        int maxRetryTimes = environment.getProperty("fast.stream.redisclustuer.maxRetryTimes", Integer.class);

        RedisService redisService = new RedisService(redisNodes, timeout, maxRetryTimes);

        return redisService;
    }

    @Bean
    public Engine engine() throws Exception {
        AutoScheduledEngine autoScheduledEngine = new AutoScheduledEngine();
        autoScheduledEngine.setApplication(environment.getProperty("application"));
        autoScheduledEngine.setMaxRetry(20);
        autoScheduledEngine.setTaskPersister(taskPersister());
        if (containsBean(TaskIDGenerator.class)) {
            autoScheduledEngine.setTaskIDGenerator(applicationContext.getBean(TaskIDGenerator.class));
        } else {
            autoScheduledEngine.setTaskIDGenerator(new UUIDTaskIDGenerator());
        }
        if (containsBean(TaskExecutor.class)) {
            autoScheduledEngine.setTaskExecutor(applicationContext.getBean(TaskExecutor.class));
        } else {
            RetryPattern retryPattern = containsBean(RetryPattern.class)
                    ? applicationContext.getBean(RetryPattern.class) : new ScheduledTimeIntervalPattern();
            autoScheduledEngine.setTaskExecutor(new ThreadPoolTaskExecutor(taskPersister(), retryPattern, graphContext(), autoScheduledEngine));
        }

        return autoScheduledEngine;
    }

    @Bean
    public MessageClient kafkaClient() {
        KafkaClientImpl kafkaClientImpl = new KafkaClientImpl();
        if (environment.getProperty("fast.stream.kafka.group") != null) {
            kafkaClientImpl.setGroup(environment.getProperty("fast.stream.kafka.group"));
        }
        kafkaClientImpl.setServers(environment.getProperty("fast.stream.kafka.servers"));
        kafkaClientImpl.setTopic(environment.getProperty("fast.stream.kafka.topic"));
        List<String> keys = new LinkedList<String>();
        keys.add(TaskCompleteEvent.class.getSimpleName());
        kafkaClientImpl.setKeys(keys);

        kafkaClientImpl.init();
        return kafkaClientImpl;
    }

    @Bean
    public EventCenter eventCenter() throws Exception {
        MemoryEventCenter eventCenter = new MemoryEventCenter();
        eventCenter.setKafkaClient(kafkaClient());
        eventCenter.setTopic(environment.getProperty("fast.stream.kafka.topic"));
        eventCenter.registerListener(TaskCompleteEvent.class, mongodbBasedEventCompleteListener());

        eventCenter.init();
        return eventCenter;
    }

    @Bean
    public Listener mongodbBasedEventCompleteListener() throws Exception {
        MangoDBBasedTaskCompleteListener mangoDBBasedTaskCompleteListener = new MangoDBBasedTaskCompleteListener();
        mangoDBBasedTaskCompleteListener.setCollectionName(environment.getProperty("application") + "_mangotable");
        mangoDBBasedTaskCompleteListener.setMongoClient(mongoClient());
        return mangoDBBasedTaskCompleteListener;
    }

    @Bean
    public MongoClient mongoClient() throws Exception {
        MongoClientImpl mongoClientImpl = new MongoClientImpl();
        String servers = environment.getProperty("fast.stream.mongo.servers");
        String[] pairs = servers.split(";");
        Map<String, Integer> mongoServers = new HashMap<String, Integer>();
        for (String pair : pairs) {
            String[] config = pair.split(":");
            mongoServers.put(config[0], Integer.parseInt(config[1]));
        }
        mongoClientImpl.init(mongoServers, environment.getProperty("fast.stream.mongo.username"),
                environment.getProperty("fast.stream.mongo.dbname"), environment.getProperty("fast.stream.mongo.passwod"));

        return mongoClientImpl;
    }

    @Bean
    public TaskStorage messageQueueBasedTaskStorage() throws Exception {
        KafkaBasedTaskStorage kafkaBasedTaskStorage = new KafkaBasedTaskStorage();
        kafkaBasedTaskStorage.setEventCenter(eventCenter());
        return kafkaBasedTaskStorage;
    }

    private <T> boolean containsBean(final Class<T> clazz) {
        try {
            applicationContext.getBean(clazz);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

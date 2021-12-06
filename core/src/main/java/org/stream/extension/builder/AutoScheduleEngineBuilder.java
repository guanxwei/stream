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

package org.stream.extension.builder;

import org.stream.core.execution.AutoScheduledEngine;
import org.stream.core.resource.ResourceCatalog;
import org.stream.extension.events.EventCenter;
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.monitor.StatusMonitor;
import org.stream.extension.persist.RedisService;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.utils.TaskIDGenerator;

public final class AutoScheduleEngineBuilder {

    private AutoScheduleEngineBuilder() { }

    private AutoScheduledEngine autoScheduledEngine;

    private TaskPersister taskPersister;

    private TaskExecutor taskExecutor;

    private TaskIDGenerator taskIDGenerator;

    private String application;

    private StatusMonitor statusMonitor;

    private ResourceCatalog resourceCatalog;

    private EventCenter eventCenter;

    private RedisService redisService;

    private int maxRetry = 20;

    public static AutoScheduleEngineBuilder builder() {
        return new AutoScheduleEngineBuilder();
    }

    public AutoScheduleEngineBuilder taskPersister(final TaskPersister taskPersister) {
        this.taskPersister = taskPersister;
        return this;
    }

    public AutoScheduleEngineBuilder taskIDGenerator(final TaskIDGenerator taskIDGenerator) {
        this.taskIDGenerator = taskIDGenerator;
        return this;
    }

    public AutoScheduleEngineBuilder application(final String application) {
        this.application = application;
        return this;
    }

    public AutoScheduleEngineBuilder taskExecutor(final TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    public AutoScheduleEngineBuilder statusMonitor(final StatusMonitor statusMonitor) {
        this.statusMonitor = statusMonitor;
        return this;
    }

    public AutoScheduleEngineBuilder resourceCatalog(final ResourceCatalog resourceCatalog) {
        this.resourceCatalog = resourceCatalog;
        return this;
    }

    public AutoScheduleEngineBuilder eventCenter(final EventCenter eventCenter) {
        this.eventCenter = eventCenter;
        return this;
    }

    public AutoScheduleEngineBuilder redisService(final RedisService redisService) {
        this.redisService = redisService;
        return this;
    }

    public AutoScheduledEngine build() {
        autoScheduledEngine = new AutoScheduledEngine();
        autoScheduledEngine.setApplication(application);
        autoScheduledEngine.setTaskExecutor(taskExecutor);
        autoScheduledEngine.setTaskIDGenerator(taskIDGenerator);
        autoScheduledEngine.setTaskPersister(taskPersister);
        autoScheduledEngine.setEventCenter(eventCenter);
        autoScheduledEngine.setResourceCatalog(resourceCatalog);
        autoScheduledEngine.setMaxRetry(maxRetry);
        statusMonitor.setTaskExecutor(taskExecutor);
        statusMonitor.setRedisService(redisService);
        return autoScheduledEngine;
    }
}

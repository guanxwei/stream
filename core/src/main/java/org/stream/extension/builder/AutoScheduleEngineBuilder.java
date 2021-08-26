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
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.monitor.StatusMonitor;
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

    public AutoScheduledEngine build() {
        this.autoScheduledEngine = new AutoScheduledEngine();
        this.autoScheduledEngine.setApplication(application);
        this.autoScheduledEngine.setTaskExecutor(taskExecutor);
        this.autoScheduledEngine.setTaskIDGenerator(taskIDGenerator);
        this.autoScheduledEngine.setTaskPersister(taskPersister);
        this.statusMonitor.setTaskExecutor(taskExecutor);
        return this.autoScheduledEngine;
    }
}

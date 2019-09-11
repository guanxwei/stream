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

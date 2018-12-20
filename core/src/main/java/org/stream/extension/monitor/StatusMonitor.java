package org.stream.extension.monitor;

import org.stream.extension.executors.TaskExecutor;

import lombok.Getter;
import lombok.Setter;

/**
 * Task executor status monitor.
 * @author 魏冠雄
 *
 */
public class StatusMonitor {

    @Setter @Getter
    private TaskExecutor taskExecutor;

    /**
     * Get active task number that being executed by the work flow engine.
     * @return Active task number.
     */
    public int activeTasks() {
        return taskExecutor.getActiveTasks();
    }

    /**
     * Get task number that being waiting to be executed by the work flow engine;
     * @return Queued task number.
     */
    public int getQueuedTasks() {
        return taskExecutor.getQueuedTasks();
    }

    /**
     * Get thread pool size.
     * @return Return thread pool size.
     */
    public int getPoolSize() {
        return taskExecutor.getPoolSize();
    }
}

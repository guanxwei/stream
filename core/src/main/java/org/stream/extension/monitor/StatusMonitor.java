package org.stream.extension.monitor;

import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.persist.QueueHelper;
import org.stream.extension.persist.RedisService;

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

    @Setter
    private RedisService redisService;

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

    /**
     * Get the quantity of suspended tasks.
     * @param application Application name.
     * @return The quantity of suspended tasks.
     */
    public long getSuspendedTaks(final String application) {
        long sum = 0;
        for (int i = 0; i < QueueHelper.DEFAULT_QUEUES; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(QueueHelper.RETRY_KEY).append(application).append("_").append(i);
            String queue = sb.toString();
            sum += redisService.getListSize(queue);
        }

        return sum;
    }
}

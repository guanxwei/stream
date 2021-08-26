package org.stream.extension.executors;

import java.util.concurrent.Future;

import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;

/**
 * Encapsulation of task executor.
 * @author 魏冠雄
 *
 */
public interface TaskExecutor {

    /**
     * Submit the given task to underlying executors.
     * @param primaryResource Primary resource to be used for the work flow instance.
     * @param task Task to be executed.
     * @param data Stream transfer with initiate information.
     * @return A future may contains the execution result.
     */
    Future<?> submit(
            final Resource primaryResource,
            final Task task,
            final StreamTransferData data);

    /**
     * Retry the pending task.
     * @param id target task id
     * @return a future contains the execution result.
     */
    Future<?> retry(final String id);

    /**
     * Get active task number that being executed by the work flow engine.
     * @return Active task number.
     */
    int getActiveTasks();

    /**
     * Get task number that being waiting to be executed by the work flow engine;
     * @return Queued task number.
     */
    int getQueuedTasks();

    /**
     * Get thread pool size.
     * @return Return thread pool size.
     */
    int getPoolSize();

    /**
     * Add shut down hook for the task executor.
     */
    void shutDownHook();
}

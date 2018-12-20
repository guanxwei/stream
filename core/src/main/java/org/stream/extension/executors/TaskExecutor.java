package org.stream.extension.executors;

import java.util.concurrent.Future;

import org.stream.core.component.Graph;
import org.stream.core.resource.Resource;
import org.stream.extension.meta.Task;

/**
 * Encapsulation of task executor.
 * @author 魏冠雄
 *
 */
public interface TaskExecutor {

    /**
     * Submit the given task to underlying executors.
     * @param graph Graph that containing the procedure definition information.
     * @param primaryResource Primary resource to be used for the work flow instance.
     * @param task Task to be executed.
     * @return
     */
    Future<?> submit(final Graph graph, final Resource primaryResource, final Task task);

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
}

package org.stream.core.test.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.stream.core.execution.Engine;
import org.stream.core.execution.ExecutionRunner;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.RetryRunner;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;
import org.stream.extension.executors.MockExecutorService;
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.persist.TaskPersister;

/**
 * Thread pool based implement of {@link TaskExecutor}.
 * @author 魏冠雄
 *
 */
public class SyncTaskExecutor implements TaskExecutor {

    private static int DEFAULT_POOL_SIZE = 100;

    private ExecutorService executorService = new MockExecutorService();

    private TaskPersister taskPersister;

    private RetryPattern retryPattern;

    private GraphContext graphContext;

    public SyncTaskExecutor(final TaskPersister taskPersister, final RetryPattern retryPattern,
            final GraphContext graphContext) {
        this(DEFAULT_POOL_SIZE, taskPersister, retryPattern, graphContext);
    }

    public SyncTaskExecutor(final int size, final TaskPersister taskPersister,
            final RetryPattern retryPattern, final GraphContext graphContext) {
        this(Executors.newFixedThreadPool(size), taskPersister, retryPattern, graphContext);
    }

    public SyncTaskExecutor(final ExecutorService executorService, final TaskPersister taskPersister,
            final RetryPattern retryPattern, final GraphContext graphContext) {
        // For sake of unit test, mock executor service should also be granted. 
        this.executorService = new MockExecutorService();
        this.taskPersister = taskPersister;
        this.retryPattern = retryPattern;
        this.graphContext = graphContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> submit(final Resource primaryResource,
            final Task task, final StreamTransferData data, final Engine engine) {
        Resource dataResource = Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSFER_DATA_REFERENCE)
                .value(data)
                .build();
        ExecutionRunner runner = new ExecutionRunner(retryPattern, graphContext,
                primaryResource, task, taskPersister, dataResource, engine);
        return executorService.submit(runner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getActiveTasks() {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) executorService;
        return pool.getActiveCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getQueuedTasks() {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) executorService;
        return pool.getQueue().size();
    }

    /**
     * {@inheritDoc}
     */
    public int getPoolSize() {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) executorService;
        return pool.getPoolSize();
    }

    @Override
    public Future<?> retry(final String id, final Engine engine) {
        return executorService.submit(new RetryRunner(id, graphContext, taskPersister, retryPattern, engine));
    }

    @Override
    public void shutDownHook() {
        // TODO Auto-generated method stub
        
    }
}

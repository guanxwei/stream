package org.stream.extension.executors;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.stream.core.component.Graph;
import org.stream.core.execution.ExecutionRunner;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.RetryRunner;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.helper.Jackson;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.persist.TaskPersister;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread pool based implement of {@link TaskExecutor}.
 * @author 魏冠雄
 *
 */
@Slf4j
public class ThreadPoolTaskExecutor implements TaskExecutor {

    private static int DEFAULT_POOL_SIZE = 100;

    private volatile boolean shuttingDown = false;

    private ExecutorService executorService;

    private TaskPersister taskPersister;

    private RetryPattern retryPattern;

    private GraphContext graphContext;

    public ThreadPoolTaskExecutor(final TaskPersister taskPersister, final RetryPattern retryPattern, final GraphContext graphContext) {
        this(DEFAULT_POOL_SIZE, taskPersister, retryPattern, graphContext);
    }

    public ThreadPoolTaskExecutor(final int size, final TaskPersister taskPersister,
            final RetryPattern retryPattern, final GraphContext graphContext) {
        this(Executors.newFixedThreadPool(size), taskPersister, retryPattern, graphContext);
    }

    public ThreadPoolTaskExecutor(final ExecutorService executorService, final TaskPersister taskPersister,
            final RetryPattern retryPattern, final GraphContext graphContext) {
        // For sake of unit test, mock executor service should also be granted. 
        if (!(executorService instanceof ThreadPoolExecutor) && !(executorService instanceof MockExecutorService)) {
            throw new RuntimeException("Thread pool executor supported only");
        }
        this.executorService = executorService;
        this.taskPersister = taskPersister;
        this.retryPattern = retryPattern;
        this.graphContext = graphContext;
        init();
    }

    /**
     * Initiation method to prepare back-end workers to process pending on retry work-flow instances.
     */
    private void init() {
        initiate(1, 100);
        initiate(2, 2500);
        initiate(3, 30000);
        shutDownHook();
    }

    private void initiate(final int type, final int time) {
        new Thread(() -> {
            while (!shuttingDown) {
                List<String> contents = new LinkedList<>();
                contents.addAll(taskPersister.getPendingList(type));
                log.info("Pending tasks [{}] loaded for type [{}]", Jackson.json(contents), type);
                if (!contents.isEmpty()) {
                    process(contents);
                }
                sleep(time);
            }
        }).start();
    }

    private void shutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shuttingDown = true;
            try {
                executorService.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Shut down hook thread [{}] is interrupted", Thread.currentThread().getName(), e);
            }
        }));
    }

    private void process(final List<String> taskIDList) {
        for (String taskID :taskIDList) {
            String content = taskPersister.get(taskID);
            if (content == null) {
                taskPersister.removeHub(taskID);
                continue;
            }
            RetryRunner worker = new RetryRunner(content, graphContext, taskPersister, retryPattern);
            executorService.submit(worker);
        }
    }

    private void sleep(final long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.warn("Thread interruped", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> submit(final Graph graph, final Resource primaryResource,
            final Task task) {
        StreamTransferData data = new StreamTransferData();
        Resource dataResource = Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE)
                .value(data)
                .build();
        ExecutionRunner runner = new ExecutionRunner(graph, retryPattern, graphContext,
                primaryResource, task, taskPersister, dataResource);
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
}

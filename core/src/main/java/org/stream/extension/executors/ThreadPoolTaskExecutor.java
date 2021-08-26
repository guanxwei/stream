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

package org.stream.extension.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.stream.core.execution.Engine;
import org.stream.core.execution.ExecutionRunner;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.RetryRunner;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.persist.TaskPersister;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread pool based implement of {@link TaskExecutor}.
 * @author weiguanxiong.
 *
 */
@Slf4j
public class ThreadPoolTaskExecutor implements TaskExecutor {

    private static final int DEFAULT_POOL_SIZE = 200;

    private ExecutorService executorService;

    private TaskPersister taskPersister;

    private RetryPattern retryPattern;

    private GraphContext graphContext;

    private Engine engine;

    public ThreadPoolTaskExecutor(final TaskPersister taskPersister,
            final RetryPattern retryPattern, final GraphContext graphContext, final Engine engine) {
        this(DEFAULT_POOL_SIZE, taskPersister, retryPattern, graphContext, engine);
    }

    public ThreadPoolTaskExecutor(final int size, final TaskPersister taskPersister,
            final RetryPattern retryPattern, final GraphContext graphContext, final Engine engine) {
        this(new ThreadPoolExecutor(size / 2, size, 10000, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>(200),
                (r, e) -> {
                    log.error("Workflow executor pool overflowed");
                }
            ), taskPersister, retryPattern, graphContext, engine);
    }

    public ThreadPoolTaskExecutor(final ExecutorService executorService, final TaskPersister taskPersister,
            final RetryPattern retryPattern, final GraphContext graphContext, final Engine engine) {
        this.executorService = executorService;
        this.taskPersister = taskPersister;
        this.retryPattern = retryPattern;
        this.graphContext = graphContext;
        this.engine = engine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> submit(
            final Resource primaryResource,
            final Task task,
            final StreamTransferData data) {
        Resource dataResource = Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE)
                .value(data)
                .build();
        ExecutionRunner runner = new ExecutionRunner(
                retryPattern,
                graphContext,
                primaryResource,
                task,
                taskPersister,
                dataResource,
                engine);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> retry(final String id) {
        RetryRunner worker = new RetryRunner(id, graphContext, taskPersister, retryPattern, engine);
        return executorService.submit(worker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                executorService.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // set interrupt flag
                log.error("Shut down hook thread [{}] is interrupted", Thread.currentThread().getName(), e);
            }
        }));
    }
}

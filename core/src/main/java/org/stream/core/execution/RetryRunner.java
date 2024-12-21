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

package org.stream.core.execution;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Node;
import org.stream.core.exception.WorkFlowExecutionException;
import org.stream.core.helper.Jackson;
import org.stream.core.helper.NodeConfiguration;
import org.stream.core.resource.Resource;
import org.stream.extension.executors.ThreadPoolTaskExecutor;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.StreamTransferDataStatus;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStatus;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.pattern.defaults.EqualTimeIntervalPattern;
import org.stream.extension.pattern.defaults.ScheduledTimeIntervalPattern;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.state.DefaultExecutionStateSwitcher;
import org.stream.extension.state.ExecutionStateSwitcher;

import lombok.extern.slf4j.Slf4j;

/**
 * Runnable implementation to process suspended tasks.
 * <p>
 *
 * Updated 2018/09/12:
 * Stream updated the max retry times to 24, which means Stream framework will retry single one node at most 24 times and
 * Stream framework will treat a suspend activity result as a failed result once max retry times reaches.
 * Stream framework will invoke {@link RetryPattern} to deduce point the next time the work-flow will be retried.
 * <p>
 * Stream provides two default implemented retry patterns, for detail please refer to {@link EqualTimeIntervalPattern} and
 * {@link ScheduledTimeIntervalPattern}.
 * <p>
 * Users can also use their own retry pattern by implements interface {@link RetryPattern} and initiate it with the 
 * {@link ThreadPoolTaskExecutor} as their implementation.
 * <p>
 * Update 20180920, users can also set a retry interval by adding configuration to the nodes in graphs, for detail please refer to
 * {@link NodeConfiguration}.
 */
@Slf4j
public class RetryRunner implements Runnable {

    private static final int MAX_RETRY = 24;

    private final String taskId;
    private final GraphContext graphContext;
    private final TaskPersister taskPersister;
    private final RetryPattern retryPattern;
    private boolean completedWithFailure = false;
    private final Engine engine;

    private final ExecutionStateSwitcher executionStateSwitcher = new DefaultExecutionStateSwitcher();

    /**
     * Constructor.
     * @param taskId taskId.
     * @param graphContext Graph context.
     * @param taskPersister {@linkplain TaskPersister} entity.
     * @param pattern Retry pattern.
     * @param engine workflow engine who submits the initial task and potentially used to trigger a child procedure.
     */
    public RetryRunner(
            final String taskId,
            final GraphContext graphContext,
            final TaskPersister taskPersister,
            final RetryPattern pattern,
            final Engine engine) {
        this.taskId = taskId;
        this.graphContext = graphContext;
        this.taskPersister = taskPersister;
        this.retryPattern = pattern;
        this.engine = engine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {

        log.info("Begin to process stuck task [{}]", taskId);
        if (!taskPersister.tryLock(taskId)) {
            log.info("Task [{}] is being processed, yield", taskId);
            return;
        }
        var task = Task.parse(taskPersister.get(taskId));
        if (!check(task)) {
            return;
        }

        var data = taskPersister.retrieveData(task.getTaskId());
        Resource primaryResource = preparePrimaryResource(data, task);

        TaskHelper.prepare(task.getGraphName(), primaryResource, graphContext);
        WorkFlowContext.attachResource(Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE)
                .value(data)
                .build());

        ActivityResult activityResult = null;
        var node = TaskHelper.deduceNode(task, graphContext);

        while (node != null && taskPersister.tryLock(task.getTaskId())) {
            log.info("Retry runner execute node [{}] for task [{}]", node.getNodeName(), task.getTaskId());
            activityResult = TaskHelper.perform(node, ActivityResult.SUSPEND);
            if (ActivityResult.SUSPEND.equals(activityResult)) {
                if (task.getRetryTimes() == MAX_RETRY) {
                    activityResult = ActivityResult.FAIL;
                    completedWithFailure = true;
                } else {
                    suspend(task, node, data, this.engine);
                    WorkFlowContext.reboot();
                    return;
                }
            }
            var taskStep = TaskExecutionUtils.constructStep(node.getGraph(), node,
                    TaskExecutionUtils.STATUS_MAPPING.get(activityResult), data, task);
            TaskHelper.updateTask(task, node, TaskStatus.PROCESSING.code());
            taskPersister.initiateOrUpdateTask(task, false, taskStep);
            node = TaskHelper.traverse(node,
                        executionStateSwitcher,
                        activityResult,
                        (engine, context, graphName) -> {
                            Resource primary = WorkFlowContext.getPrimary();
                            return engine.execute(context, graphName, primary, false);
                        },
                        graphContext,
                        engine);
        }

        if (activityResult != null) {
            if (completedWithFailure) {
                activityResult = ActivityResult.FAIL;
            }
            TaskHelper.complete(task, taskPersister, activityResult, node);
        }

        WorkFlowContext.reboot();
    }

    private boolean check(final Task task) {
        if (task == null) {
            taskPersister.removeHub(taskId);
            return false;
        }

        if (task.getNextExecutionTime() > System.currentTimeMillis()) {
            log.warn("Retry runner got stuck!");
            return false;
        }

        if (task.getStatus() == TaskStatus.COMPLETED.code() || task.getStatus() == TaskStatus.FAILED.code()) {
            taskPersister.complete(task, TaskHelper.deduceNode(task, graphContext));
            return false;
        }

        var node = TaskHelper.deduceNode(task, graphContext);
        if (node == null) {
            log.error("Graph has been upgraded, target node [{}] is missing", task.getNodeName());
            taskPersister.complete(task, null);
            return false;
        }

        return true;
    }

    private Resource preparePrimaryResource(final StreamTransferData streamTransferData, final Task task)  {
        var className = streamTransferData.as("primaryClass", String.class);
        try {
            return Resource.builder()
                   .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                   .value(Jackson.parse(task.getJsonfiedPrimaryResource(), Class.forName(className)))
                   .build();
        } catch (Exception e) {
            return null;
        }
    }

    private void suspend(final Task task, final Node node, final StreamTransferData data, final Engine engine) {
        // Persist workflow status to persistent layer.
        var taskStep = TaskExecutionUtils.constructStep(node.getGraph(), node, StreamTransferDataStatus.SUSPEND, data, task);
        task.setLastExecutionTime(System.currentTimeMillis());
        if (task.getNodeName().contentEquals(node.getNodeName())) {
            task.setRetryTimes(task.getRetryTimes() + 1);
        } else {
            task.setRetryTimes(0);
        }
        updateLastExecutionTimeAndSuspend(node, task, taskStep, engine);
    }

    private void updateLastExecutionTimeAndSuspend(final Node node, final Task task, final TaskStep taskStep,
            final Engine engine) {
        int interval = TaskHelper.getInterval(node, retryPattern, task.getRetryTimes());
        task.setNextExecutionTime(task.getLastExecutionTime() + interval);
        task.setNodeName(node.getNodeName());
        task.setStatus(TaskStatus.PENDING.code());
        taskPersister.suspend(task, interval, taskStep, node);
        TaskHelper.retryLocalIfPossible(interval, task.getTaskId(), graphContext, taskPersister, retryPattern, engine);
        log.info("Task [{}] suspended at node [{}] for [{}] times, will try again later after [{}] milliseconds",
                task.getTaskId(), node.getNodeName(), task.getRetryTimes(), interval);
    }

    /**
     * Get the time window depends on the current pattern and the times have tried.
     * @param time Times that have been tried.
     * @param retryPattern Retry pattern.
     * @return Next time window
     */
    public static int getTime(final RetryPattern retryPattern, final int time) {
        if (retryPattern != null) {
            return retryPattern.getTimeInterval(time);
        }
        throw new WorkFlowExecutionException("Retry pattern should not be null");
    }
}

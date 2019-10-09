package org.stream.core.execution;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Node;
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
 *
 *
 * Updated 2018/09/12:
 * Stream updated the max retry times to 24, which means Stream framework will retry single one node at most 24 times and
 * Stream framework will treat suspend activity result as failed result once max retry times reaches.
 * Stream framework will invoke {@link RetryPattern} to deduce the next time point the work-flow will be retried.
 *
 * Stream provides two default implemented retry patterns, for detail please refer to {@link EqualTimeIntervalPattern} and
 * {@link ScheduledTimeIntervalPattern}.
 *
 * Users can also use their own retry pattern by implements interface {@link RetryPattern} and initiate it with the 
 * {@link ThreadPoolTaskExecutor} as their implementation.
 *
 * Update 20180920, users can also set retry interval by adding configuration to the nodes in graphs, for detail please refer to
 * {@link NodeConfiguration}.
 */
@Slf4j
public class RetryRunner implements Runnable {

    private static final int MAX_RETRY = 24;

    private String content;
    private GraphContext graphContext;
    private TaskPersister taskPersister;
    private RetryPattern retryPattern;
    private boolean completedWithFailure = false;

    private ExecutionStateSwitcher executionStateSwitcher = new DefaultExecutionStateSwitcher();

    /**
     * Constructor.
     * @param content Jsonfied {@link Task} entity.
     * @param graphContext Graph context.
     * @param taskPersister {@linkplain TaskPersister} entity.
     * @param pattern Retry pattern.
     */
    public RetryRunner(final String content, final GraphContext graphContext, final TaskPersister taskPersister,
            final RetryPattern pattern) {
        this.content = content;
        this.graphContext = graphContext;
        this.taskPersister = taskPersister;
        this.retryPattern = pattern;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {

        log.info("Begin to process stuck task [{}]", content);
        Task task = Task.parse(content);
        if (!taskPersister.tryLock(task.getTaskId())) {
            log.info("Task [{}] is being processed, yield", task.getTaskId());
            return;
        }

        if (task.getStatus() == TaskStatus.COMPLETED.code() || task.getStatus() == TaskStatus.FAILED.code()) {
            taskPersister.complete(task);
            return;
        }

        Node node = TaskHelper.deduceNode(task, graphContext);
        StreamTransferData data = taskPersister.retrieveData(task.getTaskId());
        Resource primaryResource = preparePrimaryResource(data, task);

        TaskHelper.prepare(task.getGraphName(), primaryResource, graphContext);
        WorkFlowContext.attachResource(Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE)
                .value(data)
                .build());

        ActivityResult activityResult = null;
        while (node != null && taskPersister.tryLock(task.getTaskId())) {
            log.info("Retry runner execute node [{}] for task [{}]", node.getNodeName(), task.getTaskId());
            activityResult = TaskHelper.perform(node, ActivityResult.SUSPEND);
            if (ActivityResult.SUSPEND.equals(activityResult)) {
                if (task.getRetryTimes() == MAX_RETRY) {
                    activityResult = ActivityResult.FAIL;
                    completedWithFailure = true;
                } else {
                    suspend(task, node, data);
                    WorkFlowContext.reboot();
                    return;
                }
            }
            TaskStep taskStep = TaskExecutionUtils.constructStep(node.getGraph(), node,
                    TaskExecutionUtils.STATUS_MAPPING.get(activityResult), data, task);
            TaskHelper.updateTask(task, node, TaskStatus.PROCESSING.code());
            taskPersister.initiateOrUpdateTask(task, false, taskStep);
            node = TaskHelper.onCondition(node, executionStateSwitcher, activityResult, node.getGraph());
        }

        if (activityResult != null) {
            if (completedWithFailure) {
                activityResult = ActivityResult.FAIL;
            }
            TaskHelper.complete(task, data, taskPersister, activityResult);
        }

        WorkFlowContext.reboot();
    }

    private Resource preparePrimaryResource(final StreamTransferData streamTransferData, final Task task)  {
        String className = streamTransferData.as("primaryClass", String.class);
        try {
            return Resource.builder()
                   .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                   .value(Jackson.parse(task.getJsonfiedPrimaryResource(), Class.forName(className)))
                   .build();
        } catch (Exception e) {
            return null;
        }
    }

    private void suspend(final Task task, final Node node, final StreamTransferData data) {
        // Persist workflow status to persistent layer.
        TaskStep taskStep = TaskExecutionUtils.constructStep(node.getGraph(), node, StreamTransferDataStatus.SUSPEND, data, task);
        task.setLastExcutionTime(System.currentTimeMillis());
        if (task.getNodeName().contentEquals(node.getNodeName())) {
            task.setRetryTimes(task.getRetryTimes() + 1);
        } else {
            task.setRetryTimes(0);
        }
        updateLastExecutionTimeAndSuspend(node, task, taskStep);
    }

    private void updateLastExecutionTimeAndSuspend(final Node node, final Task task, final TaskStep taskStep) {
        int interval = TaskHelper.getInterval(node, retryPattern, task.getRetryTimes());
        task.setNextExecutionTime(task.getLastExcutionTime() + interval);
        task.setNodeName(node.getNodeName());
        task.setStatus(TaskStatus.PENDING.code());
        taskPersister.suspend(task, interval, taskStep);
        TaskHelper.retryLocalIfPossible(interval, task.getTaskId(), graphContext, taskPersister, retryPattern);
        log.info("Task [{}] suspended at node [{}] for [{}] times, will try again later after [{}] seconds",
                task.getTaskId(), node.getNodeName(), task.getRetryTimes(), interval);
    }

    /**
     * Get time window depends on the current pattern and the times have tried.
     * @param time Times that have been tried.
     * @param retryPattern Retry pattern.
     * @return Next time window
     */
    public static int getTime(final RetryPattern retryPattern, final int time) {
        if (retryPattern != null) {
            return retryPattern.getTimeInterval(time);
        }
        throw new RuntimeException("Retry pattern should not be null");
    }
}

package org.stream.core.execution;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Node;
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

import lombok.extern.slf4j.Slf4j;

/**
 * Runnable implementation to process pending on retry tasks.
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
        if (task.getStatus() == TaskStatus.COMPLETED.code()) {
            taskPersister.complete(task);
            return;
        }

        Node node = TaskHelper.deduceNode(task, graphContext);
        if (node == null) {
            log.error("Unvalid node, data [{}]", content);
            // 防止不断重试.
            taskPersister.complete(task);

            return;
        }

        Resource primaryResource = preparePrimaryResource(task);
        StreamTransferData data = taskPersister.retrieveData(task.getTaskId());

        TaskHelper.prepare(task.getGraphName(), primaryResource, graphContext);
        WorkFlowContext.attachResource(Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE)
                .value(data)
                .build());

        log.info("Retry workflow at node [{}]", node.getNodeName());
        ActivityResult activityResult = null;
        while (node != null && taskPersister.tryLock(task.getTaskId())) {
            log.info("Retry runner execute node [{}] for task [{}]", node.getNodeName(), task.getTaskId());
            activityResult = doRetry(node, task, data);
            if (ActivityResult.SUSPEND.equals(activityResult)) {
                break;
            }

            node = TaskHelper.traverse(activityResult, node);;
        }

        if (activityResult == ActivityResult.SUCCESS || activityResult == ActivityResult.FAIL) {
            TaskHelper.complete(task, data, taskPersister, activityResult);
        }
    }

    private ActivityResult doRetry(final Node node, final Task task, final StreamTransferData data) {

        /**
         * Before executing the activity, we'd check if the node contains asynchronous dependency nodes,
         * if yes, we should construct some asynchronous tasks then turn back to the normal procedure.
         */
        if (node.getAsyncDependencies() != null) {
            TaskHelper.setUpAsyncTasks(WorkFlowContext.provide(), node);
        }

        ActivityResult activityResult = TaskHelper.perform(node);

        if (activityResult.equals(ActivityResult.SUSPEND)) {
            if (task.getRetryTimes() == MAX_RETRY) {
                activityResult = ActivityResult.FAIL;
            } else {
                suspend(task, node, data);
                return ActivityResult.SUSPEND;
            }
        }

        TaskStep taskStep = TaskStep.builder()
                .createTime(System.currentTimeMillis())
                .graphName(node.getGraph().getGraphName())
                .jsonfiedTransferData(data.toString())
                .nodeName(node.getNodeName())
                .status(activityResult.equals(ActivityResult.SUCCESS) ? StreamTransferDataStatus.SUCCESS : StreamTransferDataStatus.FAIL)
                .taskId(task.getTaskId())
                .build();
        TaskHelper.updateTask(task, node, TaskStatus.PROCESSING.code());
        taskPersister.initiateOrUpdateTask(task, false, taskStep);

        return activityResult;
    }

    private Resource preparePrimaryResource(final Task task) {
        String primaryResourceString = task.getJsonfiedPrimaryResource();
        if (primaryResourceString != null) {
            return Resource.parse(primaryResourceString);
        }
        return null;
    }

    private void suspend(final Task task, final Node node, final StreamTransferData data) {
        // Persist workflow status to persistent layer.
        TaskStep taskStep = TaskStep.builder()
                .createTime(System.currentTimeMillis())
                .graphName(node.getGraph().getGraphName())
                .jsonfiedTransferData(data.toString())
                .nodeName(node.getNodeName())
                .status(StreamTransferDataStatus.SUSPEND)
                .taskId(task.getTaskId())
                .build();
        task.setLastExcutionTime(System.currentTimeMillis());
        if (task.getRetryTimes() == MAX_RETRY) {
            // Will not try any more.
            log.error("Max retry times reached for task [{}] at node [{}] in procedure [{}]", task.getTaskId(),
                    node.getNodeName(), node.getGraph().getGraphName());
            return;
        } else {
            if (task.getNodeName().contentEquals(node.getNodeName())) {
                task.setRetryTimes(task.getRetryTimes() + 1);
            } else {
                task.setRetryTimes(0);
            }
        }
        updateLastExecutionTimeAndSuspend(node, task, taskStep);
    }

    private void updateLastExecutionTimeAndSuspend(final Node node, final Task task, final TaskStep taskStep) {
        int interval = getTime(this.retryPattern, task.getRetryTimes());
        if (node.getIntervals() != null && node.getNextRetryInterval(task.getRetryTimes()) > 0) {
            interval = node.getNextRetryInterval(0);
        }
        task.setNextExecutionTime(task.getLastExcutionTime() + interval);
        task.setNodeName(node.getNodeName());
        task.setJsonfiedPrimaryResource(WorkFlowContext.getPrimary().toString());
        task.setStatus(TaskStatus.PENDING.code());
        taskPersister.suspend(task, interval, taskStep);
        TaskHelper.retryLocalIfPossible(interval, task.getTaskId(), graphContext, taskPersister, retryPattern);
        log.info("Task [{}] suspended at node [{}] for [{}] times, will try again later after [{}] seconds",
                task.getTaskId(), task.getRetryTimes(), interval);
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

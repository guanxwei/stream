package org.stream.core.execution;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.AsyncActivity;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.helper.ResourceHelper;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.StreamTransferDataStatus;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStatus;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.state.ExecutionStateSwitcher;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class containing utility methods to help manage the execution task info.
 * @author hzweiguanxiong
 *
 */
@Slf4j
public final class TaskHelper {

    // 本地重试队列长度为100.
    private static final ScheduledExecutorService LOCAL_RETRY_SCHEDULER = Executors.newScheduledThreadPool(100);

    private TaskHelper() { }

    /**
     * Prepare execution context for the incoming request. Auto scheduled cases should always initiate a new workflow instance.
     * @param graphName Graph name that the request asks.
     * @param primaryResource Primary resource that will be shared between all the nodes.
     * @param graphContext Graph context.
     * @return Chosen graph.
     */
    public static Graph prepare(final String graphName, final Resource primaryResource, final GraphContext graphContext) {
        WorkFlowContext.setUpWorkFlow();
        WorkFlow workflow = WorkFlowContext.provide();
        workflow.setResourceTank(new ResourceTank());
        WorkFlowContext.attachPrimaryResource(primaryResource);

        Graph graph = graphContext.getGraph(graphName);
        WorkFlowContext.visitGraph(graph);
        return graph;
    }

    /**
     * Execute the task on the node.
     * @param node Host node.
     * @param defaultResult Default result that will be return if the node throws any exception.
     * @return Execution result.
     */
    public static ActivityResult perform(final Node node, final ActivityResult defaultResult) {
        Node.CURRENT.set(node);
        TaskExecutionUtils.prepareAsyncTasks(node);
        try {
            return node.perform();
        } catch (Exception e) {
            log.warn("Fail to execute graph [{}] at node [{}] due to exeception",
                    node.getGraph().getGraphName(), node.getNodeName(), e);
            WorkFlowContext.markException(e);
            return defaultResult;
        }
    }

    /**
     * Update task.
     * @param task {@linkplain Task} instance that needs to be udpated.
     * @param node Current working on node.
     * @param status Execution status.
     */
    public static void updateTask(final Task task, final Node node, final int status) {
        task.setNodeName(node.getNodeName());
        task.setStatus(status);
        task.setLastExcutionTime(System.currentTimeMillis());
    }

    /**
     * Suspend the current work-flow and save data to Redis so that back-end runners have chances
     * to retry this procedure.
     * @param task Task to be suspended.
     * @param node Current working on node.
     * @param taskPersister Task persister.
     * @param pattern Retry suspended cases pattern.
     *
     * @return Retry interval.
     */
    public static int suspend(final Task task, final Node node, final TaskPersister taskPersister,
            final RetryPattern pattern) {
        // Persist work-flow status to persistent layer.
        StreamTransferData data = WorkFlowContext.resolve(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE,
                StreamTransferData.class);
        TaskHelper.updateTask(task, node, TaskStatus.PENDING.code());
        // Let the back-end runners have chances to retry the suspended work-flow.;
        int interval = getInterval(node, pattern, 0);
        task.setNextExecutionTime(task.getLastExcutionTime() + interval);
        TaskStep taskStep = TaskExecutionUtils.constructStep(node.getGraph(), node, StreamTransferDataStatus.SUSPEND, data, task);
        taskPersister.suspend(task, interval, taskStep);

        return interval;
    }

    /**
     * Get next retry time window to be elapsed.
     * @param node Current node.
     * @param pattern Retry pattern.
     * @param retryTimes Retry times.
     * @return Period to be elasped.
     */
    public static int getInterval(final Node node, final RetryPattern pattern, final int retryTimes) {
        int interval = RetryRunner.getTime(pattern, retryTimes);
        if (node.getIntervals() != null) {
            interval = node.getNextRetryInterval(retryTimes);
        }
        return interval;
    }

    /**
     * Mark the task as success.
     * @param task Task to be marked.
     * @param data Data to be saved.
     * @param taskPersister Task persister.
     * @param finalActivityResult Activity execution result of the last node.
     */
    public static void complete(final Task task, final StreamTransferData data, final TaskPersister taskPersister,
            final ActivityResult finalActivityResult) {
        task.setStatus(TaskStatus.COMPLETED.code());
        if (finalActivityResult == ActivityResult.FAIL) {
            task.setStatus(TaskStatus.FAILED.code());
        }

        taskPersister.persist(task);
        taskPersister.complete(task);
    }

    /**
     * Retrieve the next node to be executed based on the result the current node returned and the configuration for the current node.
     * @param activityResult The result current node returned.
     * @param startNode the current node reference.
     * @return The next node.
     */
    public static Node traverse(final ActivityResult activityResult, final Node startNode) {

        if (activityResult == null) {
            return null;
        }

        return activityResult.accept(new ActivityResult.Visitor<Node>() {
            @Override
            public Node success() {
                return startNode.getNext().onSuccess();
            }

            @Override
            public Node fail() {
                // 如果没有配置fail节点，默认使用default error node处理.循环 Default error 处理完只能返回success，否者会陷入死.
                return startNode.getNext().onFail() == null ? startNode.getGraph().getDefaultErrorNode() : startNode.getNext().onFail();
            }

            @Override
            public Node suspend() {
                return startNode.getNext().onSuspend();
            }

            @Override
            public Node check() {
                return startNode.getNext().onCheck();
            }
        });
    }

    /**
     * Set up asynchronous tasks and submit them to executor, all the work-flow instances share one asynchronous task executor, so it is expectable to
     * take some time to complete the task, some times when the traffic is busy it may take more time to complete the task than normal.
     * @param workFlow The asynchronous task belong to.
     * @param node The node that need submit asynchronous tasks.
     */
    public static void setUpAsyncTasks(final WorkFlow workFlow, final Node node) {
        node.getAsyncDependencies().forEach(async -> {
            Callable<ActivityResult> job = () -> {
                AsyncActivity asyncActivity = (AsyncActivity) async.getActivity();
                String primaryResourceReference = workFlow.getPrimary() == null ? null : workFlow.getPrimary().getResourceReference();
                asyncActivity.linkUp(workFlow.getResourceTank(), primaryResourceReference);
                asyncActivity.getNode().set(async);
                asyncActivity.getHost().set(node);
                ActivityResult activityResult = ActivityResult.FAIL;
                try {
                    activityResult = async.perform();
                } catch (Exception e) {
                    log.warn(async.getNodeName() + " async task failed for workflow [{}]", workFlow.getWorkFlowId());
                } finally {
                    asyncActivity.cleanUp();
                }
                return activityResult;
            };
            FutureTask<ActivityResult> task = new FutureTask<ActivityResult>(job);
            Resource taskWrapper = Resource.builder()
                    .value(task)
                    .resourceReference(async.getNodeName() + ResourceHelper.ASYNC_TASK_SUFFIX)
                    .build();
            workFlow.attachResource(taskWrapper);
            workFlow.addAsyncTasks(async.getNodeName() + ResourceHelper.ASYNC_TASK_SUFFIX);
            WorkFlowContext.submit(task);
        });
    }

    /**
     * Deduce re-try entrance node from the task entity.
     * @param task Task that needs to be re-ran.
     * @param graphContext Graph context that contains all the work-flow configuration.
     * @return Enrance node.
     */
    public static Node deduceNode(final Task task, final GraphContext graphContext) {
        String graphName = task.getGraphName();
        String nodeName = task.getNodeName();

        Node node = null;
        Graph graph = graphContext.getGraph(graphName);
        if (graph == null) {
            return null;
        }

        for (Node alternative : graph.getNodes()) {
            if (alternative.getNodeName().equals(nodeName)) {
                node = alternative;
                break;
            }
        }
        return node;
    }

    /**
     * Retry the suspended work-flow instance locally after scheduled delay if possible.
     * @param interval Time to delay in {@link TimeUnit#MILLISECONDS}, if it is less than 5000, Stream framework will try to run it locally to speed up.
     * @param taskID Task id.
     * @param graphContext Graph context.
     * @param taskPersister Task persister.
     * @param pattern Retry pattern.
     */
    public static void retryLocalIfPossible(final int interval, final String taskID, final GraphContext graphContext,
            final TaskPersister taskPersister, final RetryPattern pattern) {
        if (interval <= 1000) {
            LOCAL_RETRY_SCHEDULER.schedule(() -> {
                log.info("Local retry for task [{}] begin after interval [{}]", taskID, interval);
                RetryRunner retryRunner = new RetryRunner(taskID, graphContext, taskPersister, pattern);
                try {
                    retryRunner.run();
                } catch (Exception e) {
                    log.info("Error happend", e);
                }

            }, interval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Deduce the next node to be executed based on the activity result and current node's configuration.
     * If dead loop is detected, return null so that the engine can terminate the worklflow normally.
     * @param previous The node previous step was executed.
     * @param executionStateSwitcher Execution state switcher used to check if it is stuck at dead loop.
     *      if so change the next node to null.
     * @param activityResult The activity result returned by the previous node.
     * @param graph Graph
     * @return Next node to be executed.
     */
    public static Node onCondition(final Node previous, final ExecutionStateSwitcher executionStateSwitcher,
            final ActivityResult activityResult, final Graph graph) {
        Node next = TaskHelper.traverse(activityResult, previous);
        if (executionStateSwitcher.isOpen(previous, next, activityResult)) {
            next = executionStateSwitcher.open(graph, previous);
        }

        return next;
    }
}

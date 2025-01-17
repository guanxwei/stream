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

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.AsyncActivity;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.runtime.ResourceHelper;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.extension.intercept.Interceptors;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.StreamTransferDataStatus;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStatus;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.state.ExecutionStateSwitcher;
import org.stream.extension.utils.TripleFunction;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class containing utility methods to help manage the execution task info.
 * @author guanxiong wei
 *
 */
@Slf4j
public final class TaskHelper {

    private static final ThreadPoolExecutor EXECUTOR_SERVICE = new ThreadPoolExecutor(100, 100, 10 * 1000, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(30),
            (r, e) -> {
                log.error("Workflow retry executor pool overflowed");
            });

    // Set the local retry thread pool with fixed queue length 100.
    private static final ScheduledExecutorService LOCAL_RETRY_SCHEDULER = new ScheduledThreadPoolExecutor(100,
            EXECUTOR_SERVICE.getThreadFactory());

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
        TaskExecutionUtils.prepareAsyncTasks(node);
        try {
            // Invoke interceptors before we execute the actions
            Interceptors.before(node);
            ActivityResult result = null;
            if (node.isDegradable()) {
                // Run the node in sentinel mode.
                result = runInSentinelMode(node);
            } else {
                // Run the node directly.
                result = node.perform();
            }
            Interceptors.after(node, result);
            return result;
        } catch (Exception e) {
            log.warn("Fail to execute graph [{}] at node [{}] due to exception",
                    node.getGraph().getGraphName(), node.getNodeName(), e);
            WorkFlowContext.markException(e);
            Interceptors.onError(node, e);
            return defaultResult;
        }
    }

    private static ActivityResult runInSentinelMode(final Node node) {
        Entry entry = null;
        try {
            entry = SphU.entry(node.getGraph().getGraphName() + "::" + node.getNodeName());
            return node.perform();
        } catch (Exception e) {
            if (e instanceof BlockException) {
                log.error("Node [{}] is degraded", node.getNodeName());
            } else {
                Tracer.traceEntry(e, entry);
            }
            return ActivityResult.FAIL;
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
        task.setLastExecutionTime(System.currentTimeMillis());
    }

    /**
     * Suspend the current work-flow and save data to Redis so that back-end runners have chances
     * to retry this procedure.
     * @param task Task to be suspended.
     * @param node Current working on node.
     * @param taskPersister Task persister.
     * @param pattern Retry suspended case pattern.
     *
     * @return Retry interval.
     */
    public static int suspend(final Task task, final Node node, final TaskPersister taskPersister,
            final RetryPattern pattern) {
        // Persist work-flow status to persistent layer.
        StreamTransferData data = WorkFlowContext.resolve(WorkFlowContext.WORK_FLOW_TRANSFER_DATA_REFERENCE,
                StreamTransferData.class);
        TaskHelper.updateTask(task, node, TaskStatus.PENDING.code());
        int interval = getInterval(node, pattern, 0);
        task.setNextExecutionTime(task.getLastExecutionTime() + interval);
        TaskStep taskStep = TaskExecutionUtils.constructStep(node.getGraph(), node, StreamTransferDataStatus.SUSPEND, data, task);
        taskPersister.suspend(task, interval, taskStep, node);

        return interval;
    }

    /**
     * Get the next retry time window to be elapsed.
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
     * Mark the task as a success.
     * @param task target task.
     * @param taskPersister task persister used to update the task status.
     * @param finalActivityResult Activity execution result of the last node.
     * @param node last executed activity node.
     */
    public static void complete(
            final Task task,
            final TaskPersister taskPersister,
            final ActivityResult finalActivityResult,
            final Node node) {
        task.setStatus(TaskStatus.COMPLETED.code());
        if (finalActivityResult == ActivityResult.FAIL) {
            task.setStatus(TaskStatus.FAILED.code());
        }

        taskPersister.persist(task);
        taskPersister.complete(task, node);
    }

    private static Node traverse(final ActivityResult activityResult, final Node startNode,
            final TripleFunction<Engine, GraphContext, String, ResourceTank> function,
            final GraphContext context,
            final Engine engine) {

        if (activityResult == null) {
            return null;
        }

        return activityResult.accept(new ActivityResult.Visitor<Node>() {
            @Override
            public Node onSuccess() {
                return startNode.getNext().onSuccess();
            }

            @Override
            public Node onFail() {
                // Return the configured failed node, if the node is not configured return the graph default error process node.
                return startNode.getNext().onFail() == null ? startNode.getGraph().getDefaultErrorNode() : startNode.getNext().onFail();
            }

            @Override
            public Node onSuspend() {
                return startNode.getNext().onSuspend();
            }

            @Override
            public Node onCheck() {
                return startNode.getNext().onCheck();
            }

            @Override
            public Node onCondition() {
                int conditionCode = ActivityResult.CONDITION_CODE.get();
                ActivityResult.CONDITION_CODE.remove();
                return startNode.getNode(conditionCode);
            }

            @Override
            public Node onInvoke() {
                String target = ActivityResult.INVOKE_GRAPH.get();
                ActivityResult.INVOKE_GRAPH.remove();
                String graph = startNode.getSubflows().stream()
                        .filter(flow -> flow.getTarget().equals(target))
                        .findAny()
                        .get()
                        .getGraph();
                ResourceTank response = function.apply(engine, context, graph);
                Resource primary = WorkFlowContext.getPrimary();
                response.getResources().values().forEach(resource -> {
                    if (!Objects.equals(primary, resource)) {
                        WorkFlowContext.attachResource(resource);
                    }
                });
                // Jump back to the succeed node of the parent graph's last executed node.
                return onSuccess();
            }
        });
    }

    /**
     * Set up asynchronous tasks and submit them to executor, all the work-flow instances share one asynchronous task executor, so it is expected to
     * take some time to complete the task, sometimes when the traffic is busy, it may take more time to complete the task than normal.
     * @param workFlow The asynchronous task belongs to.
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
            var task = new FutureTask<>(job);
            var taskWrapper = Resource.builder()
                    .value(task)
                    .resourceReference(async.getNodeName() + ResourceHelper.ASYNC_TASK_SUFFIX)
                    .build();
            workFlow.attachResource(taskWrapper);
            workFlow.addAsyncTasks(node.getNodeName(), async.getNodeName() + ResourceHelper.ASYNC_TASK_SUFFIX);
            WorkFlowContext.submit(task);
        });
    }

    /**
     * Run daemon works.
     * @param workFlow Current running workflow instance.
     * @param node Node to be executed this round.
     */
    public static void runDaemons(final WorkFlow workFlow, final Node node) {
        node.getDaemons().forEach(async -> {
            Runnable job = () -> {
                AsyncActivity asyncActivity = (AsyncActivity) async.getActivity();
                String primaryResourceReference = workFlow.getPrimary() == null ? null : workFlow.getPrimary().getResourceReference();
                asyncActivity.linkUp(workFlow.getResourceTank(), primaryResourceReference);
                asyncActivity.getNode().set(async);
                asyncActivity.getHost().set(node);
                try {
                    asyncActivity.act();
                } catch (Throwable throwable) {
                    log.warn("Attention, daemon activity is throwing exception", throwable);
                } finally {
                    asyncActivity.cleanUp();
                }
            };
            WorkFlowContext.submit(job);
        });
    }

    /**
     * Deduce re-try entrance node from the task entity.
     * @param task Task that needs to be re-run.
     * @param graphContext Graph context that contains all the work-flow configuration.
     * @return Entrance node.
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
     * @param engine workflow engine.
     */
    public static void retryLocalIfPossible(
            final int interval,
            final String taskID,
            final GraphContext graphContext,
            final TaskPersister taskPersister,
            final RetryPattern pattern,
            final Engine engine) {
        if (interval <= 1000) {
            LOCAL_RETRY_SCHEDULER.schedule(() -> {
                log.info("Local retry for task [{}] begin after interval [{}]", taskID, interval);
                RetryRunner retryRunner = new RetryRunner(taskID, graphContext, taskPersister, pattern, engine);
                try {
                    retryRunner.run();
                } catch (Exception e) {
                    log.info("Error happened", e);
                }

            }, interval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Deduce the next node to be executed based on the activity result and current node's configuration.
     * If a dead loop is detected, return null so that the engine can terminate the workflow normally.
     * @param previous The node previous step was executed.
     * @param executionStateSwitcher Execution state switcher used to check if it is stuck at dead loop.
     *      if so, change the next node to null.
     * @param activityResult The activity result returned by the previous node.
     * @param function Function that should be applied before turning back to the caller when the previous node returned an invoke result.
     * @param context Graph context.
     * @param engine Workflow engine.
     * @return Next node to be executed.
     */
    public static Node traverse(
            final Node previous,
            final ExecutionStateSwitcher executionStateSwitcher,
            final ActivityResult activityResult,
            final TripleFunction<Engine, GraphContext, String, ResourceTank> function,
            final GraphContext context,
            final Engine engine) {
        Node next = traverse(activityResult, previous, function, context, engine);
        if (executionStateSwitcher.isOpen(previous, next, activityResult)) {
            next = executionStateSwitcher.open(previous.getGraph(), previous);
        }

        return next;
    }
}

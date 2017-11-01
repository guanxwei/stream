package org.stream.core.execution;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.AsyncActivity;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.helper.Jackson;
import org.stream.core.helper.ResourceHelper;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.persist.TaskPersister;

/**
 * Task helper.
 * @author hzweiguanxiong
 *
 */
public final class TaskHelper {

    private TaskHelper() { }

    /**
     * Prepare execution context for the incoming request.
     * @param graphName Graph name that the request asks.
     * @param primaryResource Primary resource that will be shared between all the nodes.
     * @param graphContext Graph context.
     * @return Chosen graph.
     */
    public static Graph prepare(final String graphName, final Resource primaryResource, final GraphContext graphContext) {
        if (!WorkFlowContext.isThereWorkingWorkFlow()) {
            WorkFlowContext.setUpWorkFlow();
        }
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
     * @return Execution result.
     */
    public static ActivityResult perform(final Node node) {
        ActivityResult activityResult = null;
        try {
            activityResult = node.perform();
        } catch (Exception e) {
            activityResult = ActivityResult.SUSPEND;
        }
        return activityResult;
    }

    /**
     * Update task.
     * @param task {@linkplain Task} instance that needs to be udpated.
     * @param node Current working on node.
     * @param status Execution status.
     */
    public static void updateTask(final Task task, final Node node, final String status) {
        task.setNodeName(node.getNodeName());
        task.setJsonfiedPrimaryResource(Jackson.json(WorkFlowContext.getPrimary()));
        StreamTransferData data = (StreamTransferData) WorkFlowContext.getPrimary().getValue();
        task.setJsonfiedTransferData(data.toString());
        task.setRetryTimes(0);
        task.setStatus(status);
    }

    /**
     * Suspend the current work-flow and save data to Redis so that back-end runners have chances
     * to retry this procedure.
     * @param task Task to be suspended.
     * @param node Current working on node.
     * @param primaryResource Primary resource that will be recovered by back-end runner.
     * @param taskPersister Task persister.
     * @param pattern Retry suspended cases pattern.
     */
    public static void suspend(final Task task, final Node node, final Resource primaryResource, final TaskPersister taskPersister,
            final String pattern) {
        // Persist work-flow status to persistent layer.
        StreamTransferData data = (StreamTransferData) WorkFlowContext.getPrimary().getValue();
        task.setNodeName(node.getNodeName());
        task.setJsonfiedPrimaryResource(primaryResource.toString());
        task.setJsonfiedTransferData(data.toString());
        task.setStatus("PendingOnRetry");
        task.setRetryTimes(0);
        // Let the back-end runners have chances to retry the suspended work-flow.
        taskPersister.suspend(task, RetryRunner.getTime(0, pattern));
    }

    /**
     * Mark the task as success.
     * @param task Task to be marked.
     * @param data Data to be saved.
     * @param taskPersister Task persister.
     */
    public static void complete(final Task task, final StreamTransferData data, final TaskPersister taskPersister) {
        task.setJsonfiedTransferData(data.toString());
        task.setStatus("Completed");
        taskPersister.complete(task);
        taskPersister.persist(task);
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
                return startNode.getNext().onFail();
            }

            @Override
            public Node suspend() {
                return startNode.getNext().onSuspend();
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
                return async.perform();
            };
            FutureTask<ActivityResult> task = new FutureTask<ActivityResult>(job);
            Resource taskWrapper = Resource.builder()
                    .value(task)
                    .resourceType(node.getGraph().getResourceType())
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
        for (Node alternative : graph.getNodes()) {
            if (alternative.getNodeName().equals(nodeName)) {
                node = alternative;
                break;
            }
        }
        return node;
    }
}

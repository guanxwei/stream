package org.stream.core.execution;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.StreamTransferDataStatus;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.persist.TaskPersister;

import lombok.extern.slf4j.Slf4j;

/**
 * Runnable implementation to do the real job for the incoming request.
 */
@Slf4j
public class ExecutionRunner implements Runnable {

    private GraphContext graphContext;
    private Graph graph;
    private Resource primaryResource;
    private Task task;
    private TaskPersister taskPersister;
    private RetryPattern pattern;
    private Resource dataResource;

    /**
     * Constructor.
     * @param graph Target graph.
     * @param pattern Retry suspended cases pattern.
     * @param graphContext Graph context.
     * @param primaryResource Primary resource of this execution task.
     * @param task Stream execution task.
     * @param taskPersister Task persister.
     */
    public ExecutionRunner(final Graph graph, final RetryPattern pattern, final GraphContext graphContext,
            final Resource primaryResource, final Task task, final TaskPersister taskPersister, final Resource dataResource) {
        this.graph = graph;
        this.primaryResource = primaryResource;
        this.task = task;
        this.taskPersister = taskPersister;
        this.pattern = pattern;
        this.dataResource = dataResource;
        this.graphContext = graphContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        WorkFlowContext.setUpWorkFlow().start();
        if (dataResource != null) {
            WorkFlowContext.attachResource(dataResource);
        }
        WorkFlowContext.attachPrimaryResource(primaryResource);
        Node node = graph.getStartNode();

        while (node != null && !WorkFlowContext.provide().isRebooting()
                && taskPersister.tryLock(task.getTaskId())) {

            Node.CURRENT.set(node);

            /**
             * Before executing the activity, we'd check if the node contains asynchronous dependency nodes,
             * if yes, we should construct some asynchronous tasks then turn back to the normal procedure.
             */
            if (node.getAsyncDependencies() != null) {
                TaskHelper.setUpAsyncTasks(WorkFlowContext.provide(), node);
            }

            log.trace("Execute graph [{}] at node [{}]", graph.getGraphName(), node.getNodeName());
            ActivityResult activityResult = TaskHelper.perform(node);
            log.trace("Execution result [{}]", activityResult.name());

            if (activityResult.equals(ActivityResult.SUSPEND)) {
                int interval = TaskHelper.suspend(task, node, primaryResource, taskPersister, pattern);
                log.info("Task suspended, will try to run locally if possible");
                TaskHelper.retryLocalIfPossible(interval, task.getTaskId(), graphContext, taskPersister, pattern);
                log.info("Task [{}] suspended for interval [{}] at node [{}]", task.toString(), interval, Node.CURRENT.get().getNodeName());
                return;
            }

            StreamTransferData data = (StreamTransferData) WorkFlowContext.resolveResource(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE).getValue();
            TaskHelper.updateTask(task, node, "Executing");
            TaskStep taskStep = TaskStep.builder()
                    .createTime(System.currentTimeMillis())
                    .graphName(graph.getGraphName())
                    .jsonfiedTransferData(data.toString())
                    .nodeName(node.getNodeName())
                    .status(activityResult.equals(ActivityResult.SUCCESS) ? StreamTransferDataStatus.SUCCESS : StreamTransferDataStatus.FAIL)
                    .taskId(task.getTaskId())
                    .build();
            taskPersister.setHub(task.getTaskId(), task, false, taskStep);
            node = TaskHelper.traverse(activityResult, node);
        }

        Resource dataResource = WorkFlowContext.resolveResource(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE);
        TaskHelper.complete(task, (StreamTransferData) dataResource.getValue(), taskPersister);
    }
}

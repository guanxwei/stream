package org.stream.core.execution;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.persist.TaskPersister;

/**
 * Runnable implementation to do the real job for the incoming request.
 */
public class ExecutionRunner implements Runnable {

    private GraphContext graphContext;
    private String graphName;
    private Resource primaryResource;
    private Task task;
    private TaskPersister taskPersister;
    private String pattern;

    /**
     * Constructor.
     * @param graphContext Stream work-flow graph context.
     * @param graphName Graph's name to be executed on.
     * @param pattern Retry suspended cases pattern.
     * @param primaryResource Primary resource of this execution task.
     * @param task Stream execution task.
     * @param taskPersister Task persister.
     */
    public ExecutionRunner(final GraphContext graphContext, final String graphName, final String pattern,
            final Resource primaryResource, final Task task, final TaskPersister taskPersister) {
        this.graphContext = graphContext;
        this.graphName = graphName;
        this.primaryResource = primaryResource;
        this.task = task;
        this.taskPersister = taskPersister;
        this.pattern = pattern;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Graph graph = TaskHelper.prepare(graphName, primaryResource, graphContext);
        Node node = graph.getStartNode();

        while (node != null && !WorkFlowContext.provide().isRebooting()) {
            /**
             * Before executing the activity, we'd check if the node contains asynchronous dependency nodes,
             * if yes, we should construct some asynchronous tasks then turn back to the normal procedure.
             */
            if (node.getAsyncDependencies() != null) {
                TaskHelper.setUpAsyncTasks(WorkFlowContext.provide(), node);
            }

            ActivityResult activityResult = TaskHelper.perform(node);

            if (activityResult.equals(ActivityResult.SUSPEND)) {
                TaskHelper.suspend(task, node, primaryResource, taskPersister, pattern);
                return;
            }

            TaskHelper.updateTask(task, node, "Executing");
            taskPersister.setHub(task.getTaskId(), task.toString());
            node = TaskHelper.traverse(activityResult, node);
        }

        TaskHelper.complete(task, (StreamTransferData) WorkFlowContext.getPrimary().getValue(), taskPersister);
    }
}

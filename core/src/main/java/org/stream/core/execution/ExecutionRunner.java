package org.stream.core.execution;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.state.DefaultExecutionStateSwitcher;
import org.stream.extension.state.ExecutionStateSwitcher;

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

    private ExecutionStateSwitcher executionStateSwitcher = new DefaultExecutionStateSwitcher();

    /**
     * Constructor.
     * @param graph Target graph.
     * @param pattern Retry suspended cases pattern.
     * @param graphContext Graph context.
     * @param primaryResource Primary resource of this execution task.
     * @param task Stream execution task.
     * @param taskPersister Task persister.
     * @param dataResource A pointer the input task resource.
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
        WorkFlowContext.attachResource(dataResource);
        WorkFlowContext.attachPrimaryResource(primaryResource);
        Node node = graph.getStartNode();

        ActivityResult activityResult = null;
        while (node != null && taskPersister.tryLock(task.getTaskId())) {
            log.trace("Execute graph [{}] at node [{}]", graph.getGraphName(), node.getNodeName());
            activityResult = TaskHelper.perform(node, ActivityResult.SUSPEND);
            log.trace("Execution result [{}]", activityResult.name());

            if (activityResult.equals(ActivityResult.SUSPEND)) {
                log.info("Task suspended, will try to run locally if possible");
                TaskExecutionUtils.suspend(task, node, taskPersister, graph, pattern, graphContext);
                return;
            }

            Node temp = node;
            node = TaskExecutionUtils.updateTaskAndTraverseNode(task, node, taskPersister, graph, activityResult);

            if (executionStateSwitcher.isOpen(temp, node, activityResult)) {
                node = executionStateSwitcher.open(graph, temp);
            }
        }

        TaskHelper.complete(task, WorkFlowContext.resolve(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE, StreamTransferData.class),
                taskPersister, activityResult);
        WorkFlowContext.reboot();
    }

}

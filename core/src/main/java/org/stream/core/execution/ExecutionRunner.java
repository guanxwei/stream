package org.stream.core.execution;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.resource.Resource;
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
    private Resource primaryResource;
    private Task task;
    private TaskPersister taskPersister;
    private RetryPattern pattern;
    private Resource dataResource;
    private Engine engine;

    private ExecutionStateSwitcher executionStateSwitcher = new DefaultExecutionStateSwitcher();

    /**
     * Constructor.
     * @param pattern Retry suspended cases pattern.
     * @param graphContext Graph context.
     * @param primaryResource Primary resource of this execution task.
     * @param task Stream execution task.
     * @param taskPersister Task persister.
     * @param dataResource A pointer the input task resource.
     * @param engine Workflow execution engine.
     */
    public ExecutionRunner(
            final RetryPattern pattern,
            final GraphContext graphContext,
            final Resource primaryResource,
            final Task task,
            final TaskPersister taskPersister,
            final Resource dataResource,
            final Engine engine) {
        this.primaryResource = primaryResource;
        this.task = task;
        this.taskPersister = taskPersister;
        this.pattern = pattern;
        this.dataResource = dataResource;
        this.graphContext = graphContext;
        this.engine = engine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        WorkFlowContext.setUpWorkFlow().start();
        WorkFlowContext.attachResource(dataResource);
        WorkFlowContext.attachPrimaryResource(primaryResource);
        Graph graph = graphContext.getGraph(task.getGraphName());
        Node node = graph.getStartNode();
        Node last = null;

        ActivityResult activityResult = null;
        while (node != null && taskPersister.tryLock(task.getTaskId())) {
            log.trace("Execute graph [{}] at node [{}]", graph.getGraphName(), node.getNodeName());
            activityResult = TaskHelper.perform(node, ActivityResult.SUSPEND);
            log.trace("Execution result [{}]", activityResult.name());

            if (activityResult.equals(ActivityResult.SUSPEND)) {
                log.info("Task suspended, will try to run locally if possible");
                TaskExecutionUtils.suspend(task, node, taskPersister, pattern, graphContext, this.engine);
                return;
            }

            TaskExecutionUtils.updateTask(task, node, taskPersister, graph, activityResult);
            last = node;
            node = TaskHelper.traverse(node,
                        executionStateSwitcher,
                        activityResult,
                        (engine, context, graphName) -> {
                            Resource primary = WorkFlowContext.getPrimary();
                            return engine.execute(context, graphName, primary, false);
                        },
                        graphContext,
                        this.engine);
        }

        TaskHelper.complete(task, taskPersister, activityResult, last);
        WorkFlowContext.reboot();
    }

}

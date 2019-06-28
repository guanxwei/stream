package org.stream.core.execution;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang3.StringUtils;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.AsyncActivity;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.WorkFlow.WorkFlowStatus;
import org.stream.core.helper.ResourceHelper;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.TimeOut;
import org.stream.extension.circuit.DefaultExecutionStateSwitcher;
import org.stream.extension.circuit.ExecutionStateSwitcher;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@linkplain Engine}.
 * All the work will be executed in single host and if any exception happends, retry procedure will not be applied.
 *
 */
@Slf4j
public class DefaultEngine implements Engine {

    private static final  ThreadLocal<Integer> ENTRANCE_TAG = new ThreadLocal<>();

    static {
        ENTRANCE_TAG.set(0);
    }

    @Setter
    private ExecutionStateSwitcher executionStateSwitcher = new DefaultExecutionStateSwitcher();

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext,
            final String graphName,
            final boolean autoRecord) {

        return start(graphContext, graphName, null, autoRecord, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext,
            final String graphName,
            final Resource primaryResource,
            final boolean autoRecord) {

        return start(graphContext, graphName, primaryResource, autoRecord, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext,
            final String graphName,
            final Resource primaryResource,
            final boolean autoRecord) {

        return  start(graphContext, graphName, primaryResource, autoRecord, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext,
            final String graphName,
            final boolean autoRecord) {

        return start(graphContext, graphName, null, autoRecord, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reboot() throws InterruptedException {
        WorkFlowContext.reboot();
    }

    /**
     * {@inheritDoc}
     */
    public void waitAndReboot() {
        WorkFlowContext.provide().setRebooting(true);
    }

    private ResourceTank start(final GraphContext graphContext,
            final String graphName,
            final Resource resource,
            final boolean autoRecord,
            final boolean autoClean) {

        // Deduce work-flow procedure definition graph.
        Graph graph = deduceGraph(graphName, graphContext);

        // Pre-set work-flow context flags.
        boolean isWorkflowEntryGraph = false;
        if (ENTRANCE_TAG.get() == null || ENTRANCE_TAG.get() == 0) {
            isWorkflowEntryGraph = true;
            ENTRANCE_TAG.set(1);
        }

        // Prepare work-flow runtime context.
        WorkFlow context = prepare(graph, autoRecord, graphName, resource, isWorkflowEntryGraph);

        // Execute
        execute(context, graph, autoRecord);
        ResourceTank resourceTank = context.getResourceTank();

        // Clean context.
        clear(context, autoClean, isWorkflowEntryGraph);

        // Return.
        return resourceTank;
    }

    private Graph deduceGraph(final String graphName, final GraphContext graphContext) {
        Graph graph = graphContext.getGraph(graphName);
        if (graph == null) {
            throw new WorkFlowExecutionExeception("Graph is not present! Please double check the graph name you provide.");
        }
        return graph;
    }

    private void clearRelationship(final WorkFlow workFlow) {
        if (workFlow.getParent() != null) {
            workFlow.getParent().getChildren().remove(workFlow);
            WorkFlowContext.reboot();
            workFlow.setParent(null);
        }
    }

    private WorkFlow prepare(final Graph graph, final boolean autoRecord, final String graphName, final Resource resource,
            final boolean isWorkflowEntryGraph) {
        WorkFlow workFlow;
        if (!WorkFlowContext.isThereWorkingWorkFlow()) {
            //Currently there is no working work-flow in the same thread, we should create a new work-flow.
            log.info("New workflow instance will be initiated for graph [{}] with resource [{}]", graphName,
                    resource == null ? StringUtils.EMPTY : resource.toString());
            workFlow = initiate(graph, autoRecord, graphName);
            log.info("New work flow instance [{}] initiated", workFlow.getWorkFlowId());
        } else {
            /**
             * Current there is work-flow instance attached to the thread, just reuse it.
             * Before use the work-flow instance we'd check the status first, if it is in Working status,
             * we should create a new sub instance and mark the current work-flow instance as father work-flow.
             */
            workFlow = WorkFlowContext.provide();
            log.info("Pre-created workflow instance [{}] will be reused for graph [{}] with resource [{}]",
                    workFlow.getWorkFlowId(), graphName, resource == null ? StringUtils.EMPTY : resource.toString());

            refresh(workFlow, graph, autoRecord, isWorkflowEntryGraph);
        }
        workFlow = WorkFlowContext.provide();
        workFlow.attachPrimaryResource(resource);
        return workFlow;
    }

    private WorkFlow initiate(final Graph graph, final boolean autoRecord, final String graphName) {
        //Currently there is no working work-flow in the same thread, we should create a new work-flow.
        WorkFlow workFlow = WorkFlowContext.setUpWorkFlow();
        workFlow.start();
        workFlow.visitGraph(graph);
        if (autoRecord) {
            ExecutionRecord record = ExecutionRecord.builder()
                    .time(workFlow.getCreateTime())
                    .description(String.format("Create a new workflow [%s] to execute the graph [%s]", workFlow.getWorkFlowName(), graphName))
                    .build();
            workFlow.keepRecord(record);
        }
        return workFlow;
    }

    private void refresh(final WorkFlow workFlow, final Graph graph, final boolean autoRecord,
            final boolean isWorkflowEntryGraph) {
        if (workFlow.getStatus().equals(WorkFlowStatus.CLOSED)) {
            throw new WorkFlowExecutionExeception("The workflow has been closed!");
        }

        if (workFlow.getStatus().equals(WorkFlowStatus.WAITING)) {
            useCurrentWorkflowDirectly(workFlow, graph, isWorkflowEntryGraph, autoRecord);
        } else {
            useCurrentWorkflowAsParentWorkflow(workFlow, graph, autoRecord, workFlow.getGraphContext());
        }
    }

    private void useCurrentWorkflowDirectly(final WorkFlow workFlow, final Graph graph,
            final boolean isWorkflowEntryGraph, final boolean autoRecord) {
        workFlow.setStatus(WorkFlowStatus.WORKING);
        workFlow.visitGraph(graph);
        if (autoRecord) {
            ExecutionRecord record = ExecutionRecord.builder()
                    .time(workFlow.getCreateTime())
                    .description(String.format("Add a new graph [%s] to the existed workflow [%s]", graph.getGraphName(), workFlow.getWorkFlowName()))
                    .build();
            workFlow.keepRecord(record);
        }
        workFlow.setPrimaryResourceReference(null);
        workFlow.getRecords().clear();
        workFlow.getAsyncTaksReferences().clear();
        workFlow.setResourceTank(new ResourceTank());
    }

    private void useCurrentWorkflowAsParentWorkflow(final WorkFlow workFlow, final Graph graph,
            final boolean autoRecord, final GraphContext graphContext) {

        /**
         * Currently the work-flow is running for another task, we are triggered within the running work-flow context.
         * So we should be treated as sub-workflow.
         * 
         */
        WorkFlow child = WorkFlowContext.setUpWorkFlow();
        child.start();
        child.setParent(workFlow);
        child.visitGraph(graph);
        workFlow.getChildren().add(child);
    }

    private void execute(final WorkFlow workFlow, final Graph graph, final boolean autoRecord) {

        /**
         * Extract the start node of the graph, and invoke the perform() method.
         */
        Node executionNode = graph.getStartNode();
        Node.CURRENT.set(null);
        while (executionNode != null && !WorkFlowContext.provide().isRebooting()) {

            if (isStuckInDeadLoop(executionNode, Node.CURRENT.get())) {
                WorkFlowContext.markException(new WorkFlowExecutionExeception("Next execution node should not be the same with the previous one."));
                break;
            }

            // 设置当前执行节点索引.
            Node.CURRENT.set(executionNode);

            if (autoRecord) {
                ExecutionRecord executionRecord = ExecutionRecord.builder()
                        .time(Calendar.getInstance().getTime())
                        .description(String.format("Begin to enter the node [%s]", executionNode.getNodeName()))
                        .build();
                workFlow.keepRecord(executionRecord);
            }

            /**
             * Before executing the activity, we'd check if the node contains asynchronous dependency nodes,
             * if yes, we should construct some asynchronous tasks then turn back to the normal procedure.
             */
            if (executionNode.getAsyncDependencies() != null) {
                setUpAsyncTasks(workFlow, executionNode);
            }

            ActivityResult activityResult = doExexute(executionNode);

            if (ActivityResult.SUSPEND.equals(activityResult)) {
                activityResult = processSuspendCase(activityResult, workFlow);
            }

            Node temp = executionNode;
            executionNode = traverse(activityResult, executionNode);
            if (executionStateSwitcher.isOpen(temp, executionNode, activityResult)) {
                executionNode = executionStateSwitcher.open(graph, temp);
            }
        }
    }

    private boolean isStuckInDeadLoop(final Node next, final Node previous) {
        return next.equals(previous);
    }

    private ActivityResult doExexute(final Node executionNode) {
        try {
            return executionNode.perform();
        } catch (Exception e) {
            WorkFlowContext.markException(e);
            return ActivityResult.FAIL;
        }
    }

    private ActivityResult processSuspendCase(final ActivityResult activityResult, final WorkFlow workFlow) {

        /**
         * Since the previous node return Suspend result, work-flow should suspend and wait for some time to invoke the next node.
         * Waiting time is specified by the activity himself, stored in the resource tank with a standard resource reference WAITING_TIME.
         */
        Resource timeOut = workFlow.resolveResource(TimeOut.TIME_OUT_REFERENCE);
        Long interval = timeOut.resolveValue(Long.class);
        try {
            Thread.sleep(interval.longValue());
            return ActivityResult.SUCCESS;
        } catch (InterruptedException interruptedException) {
            ExecutionRecord record = ExecutionRecord.builder()
                    .time(Calendar.getInstance().getTime())
                    .description(String.format("Thread was interupted due to [%s]", interruptedException.getMessage()))
                    .build();
            workFlow.keepRecord(record);
        }

        return ActivityResult.SUSPEND;
    }

    /**
     * Retrieve the next node to be executed based on the result the current node returned and the configuration of the current node.
     * @param activityResult The result current node returned.
     * @param startNode the current node reference.
     * @return
     */
    private Node traverse(final ActivityResult activityResult, final Node startNode) {

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
     * Set up asynchronous tasks and submit them to executor.
     * All the work-flow instances share one asynchronous task executor, so it is expectable to
     * take some time to complete the task, sometimes when the traffic is busy it may take more
     * time to complete the task than normal cases.
     * @param workFlow The asynchronous task belong to.
     * @param node The node that need submit asynchronous tasks.
     */
    private void setUpAsyncTasks(final WorkFlow workFlow, final Node node) {
        node.getAsyncDependencies().forEach(async -> {
            Callable<ActivityResult> job = () -> {
                AsyncActivity asyncActivity = (AsyncActivity) async.getActivity();
                String primaryResourceReference = workFlow.getPrimary() == null ? null : workFlow.getPrimary().getResourceReference();
                asyncActivity.linkUp(workFlow.getResourceTank(), primaryResourceReference);
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
     * Clean work-flow context before exiting.
     * @param context Work-flow runtime context.
     * @param autoClean Flag indicating if we need shut the main flow instance.
     * @param isWorkflowEntryGraph Flag indicating if the current work-flow instance is at the top of
     *      work-flow hierarchy.
     */
    private void clear(final WorkFlow context, final boolean autoClean, final boolean isWorkflowEntryGraph) {
        context.setStatus(WorkFlowStatus.WAITING);
        if (isWorkflowEntryGraph) {
            // Make the work-flow reusable.
            ENTRANCE_TAG.set(0);
            context.setChildren(new LinkedList<>());
            executionStateSwitcher.clear();
        }
        clearRelationship(context);
        if (autoClean && isWorkflowEntryGraph) {
            /**
             * Clean up the work-flow.
             * This method will be invoked when the customers want the work-flow engine to
             * help them clean up the work-flow automatically after the
             * work-flow is executed.
             *
             * Release the connection between the resourceTank and the work-flow instance.
             * Since the invoker still hold the reference to the returned resourceTank, they can
             * retrieve resources directly from the resourceTank anyway, so it's okay rebooting here.
             * After execution, the GC will have opportunity to recycle the memory.
             */
            WorkFlowContext.reboot();
        }
    }
}

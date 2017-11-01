package org.stream.core.execution;

import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang3.tuple.Pair;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.AsyncActivity;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.WorkFlow.WorkFlowStatus;
import org.stream.core.helper.ResourceHelper;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.ResourceType;
import org.stream.core.resource.TimeOut;

/**
 * Default implementation of {@linkplain Engine}.
 *
 */
public class DefaultEngine implements Engine {

    private static final  ThreadLocal<Integer> ENTRANCE_TAG = new ThreadLocal<>();

    static {
        ENTRANCE_TAG.set(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext,
            final String graphName,
            final boolean autoRecord,
            final ResourceType resourceType) {

        return start(graphContext, graphName, null, autoRecord, resourceType, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext,
            final String graphName,
            final Resource primaryResource,
            final boolean autoRecord,
            final ResourceType resourceType) {

        return start(graphContext, graphName, primaryResource, autoRecord, resourceType, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext,
            final String graphName,
            final Resource primaryResource,
            final boolean autoRecord,
            final ResourceType resourceType) {

        return  start(graphContext, graphName, null, autoRecord, resourceType, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext,
            final String graphName,
            final boolean autoRecord,
            final ResourceType resourceType) {

        return start(graphContext, graphName, null, autoRecord, resourceType, true);
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
            final ResourceType resourceType,
            final boolean autoClean) {

        Graph graph = graphContext.getGraph(graphName);
        if (graph == null) {
            throw new WorkFlowExecutionExeception("Graph is not present! Please double check the graph name you provide.");
        }
        if (!graph.getResourceType().equals(resourceType)) {
            throw new WorkFlowExecutionExeception("The resourceType does not match the the specified one in the definition file");
        }

        boolean isWorkflowEntryGraph = false;
        if (ENTRANCE_TAG.get() == null || ENTRANCE_TAG.get() == 0) {
            isWorkflowEntryGraph = true;
            ENTRANCE_TAG.set(1);
        }

        Pair<WorkFlow, Boolean> context = prepare(graph, autoRecord, graphName, resource, isWorkflowEntryGraph);
        WorkFlow workFlow = context.getLeft();
        isWorkflowEntryGraph = context.getRight();

        execute(workFlow, graph, autoRecord);

        ResourceTank resourceTank = workFlow.getResourceTank();
        if (autoClean && isWorkflowEntryGraph) {
            /**
             * Clean up the work-flow.
             * This method will be invoked when the customers want the work-flow engine to
             * help them clean up the work-flow automatically after the
             * work-flow is executed successfully.
             *
             * Release the connection between the resourceTank and the work-flow instance.
             * Since the invoker still hold the reference to the return resourceTank, they can
             * retrieve resources directly from the resourceTank anyway.
             * After execution, the GC can have opportunity to clean up the memory.
             */
            //workFlow.setResourceTank(null);
            WorkFlowContext.reboot();
        }

        if (isWorkflowEntryGraph) {
            // Make the work-flow reusable.
            ENTRANCE_TAG.set(0);
        }
        return resourceTank;
    }

    private Pair<WorkFlow, Boolean> prepare(final Graph graph, final boolean autoRecord, final String graphName, final Resource resource,
            final boolean isWorkflowEntryGraph) {
        WorkFlow workFlow;
        boolean entrance = false;
        if (!WorkFlowContext.isThereWorkingWorkFlow()) {
            //Currently there is no working work-flow in the same thread, we should create a new work-flow.
            workFlow = initiate(graph, autoRecord, graphName);
            entrance = true;
        } else {
            //Current there is one working work-flow instance attached to the thread, just reuse it.
            workFlow = WorkFlowContext.provide();
            refresh(workFlow, graph, autoRecord, graphName, isWorkflowEntryGraph);
        }
        workFlow.attachPrimaryResource(resource);
        return Pair.of(workFlow, entrance);
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

    private void refresh(final WorkFlow workFlow, final Graph graph, final boolean autoRecord, final String graphName, final boolean isWorkflowEntryGraph) {
        if (workFlow.getStatus().equals(WorkFlowStatus.CLOSED)) {
            throw new WorkFlowExecutionExeception("The workflow has been closed!");
        }
        workFlow.visitGraph(graph);
        if (autoRecord) {
            ExecutionRecord record = ExecutionRecord.builder()
                    .time(workFlow.getCreateTime())
                    .description(String.format("Add a new graph [%s] to the existed workflow [%s]", graphName, workFlow.getWorkFlowName()))
                    .build();
            workFlow.keepRecord(record);
        }
        if (isWorkflowEntryGraph) {
            workFlow.setPrimaryResourceReference(null);
            workFlow.getRecords().clear();
            workFlow.setResourceTank(new ResourceTank());
        }
    }

    private void execute(final WorkFlow workFlow, final Graph graph, final boolean autoRecord) {
        /**
         * Extract the start node of the graph, and invoke the perform() method.
         */
        Node startNode = graph.getStartNode();
        while (startNode != null && !WorkFlowContext.provide().isRebooting()) {

            if (autoRecord) {
                ExecutionRecord executionRecord = ExecutionRecord.builder()
                        .time(Calendar.getInstance().getTime())
                        .description(String.format("Begin to enter the node [%s]", startNode.getNodeName()))
                        .build();
                workFlow.keepRecord(executionRecord);
            }

            /**
             * Before executing the activity, we'd check if the node contains async dependency nodes,
             * if yes, we should construct some async tasks then turn back to the normal procedure.
             */
            if (startNode.getAsyncDependencies() != null) {
                setUpAsyncTasks(workFlow, startNode);
            }

            ActivityResult activityResult = null;
            try {
                activityResult = startNode.perform();
            } catch (Exception e) {
                // Make sure this thread can be reused if any Exception is thrown during processing.
                ENTRANCE_TAG.set(0);
                throw e;
            }
            startNode = traverse(activityResult, startNode);

            if (activityResult.equals(ActivityResult.SUSPEND)) {
                /**
                 * Since the previous node return Suspend result, work-flow should suspend and wait for some time to invoke the next node.
                 * Waiting time is specified by the activity himself, stored in the resource tank with a standard resource reference WAITING_TIME.
                 */
                Resource timeOut = workFlow.resolveResource(TimeOut.TIME_OUT_REFERENCE);
                Long interval = (Long) timeOut.getValue();
                try {
                    Thread.sleep(interval.longValue());
                } catch (InterruptedException interruptedException) {
                    ExecutionRecord record = ExecutionRecord.builder()
                            .time(Calendar.getInstance().getTime())
                            .description(String.format("Thread was interupted due to [%s]", interruptedException.getMessage()))
                            .build();
                    workFlow.keepRecord(record);
                }
            }
        }
    }
    /**
     * Retrieve the next node to be executed based on the result the current node returned and the configuration for the current node.
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
                return startNode.getNext().onFail();
            }

            @Override
            public Node suspend() {
                return startNode.getNext().onSuspend();
            }
        });
    }

    /**
     * Set up async tasks and submit them to executor, all the work-flow instances share one async task executor, so it is expectable to
     * take some time to complete the task, some times when the traffic is busy it may take more time to complete the task than normal.
     * @param workFlow The async task belong to.
     * @param node The node that need submit async tasks.
     */
    private void setUpAsyncTasks(final WorkFlow workFlow, final Node node) {
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
}

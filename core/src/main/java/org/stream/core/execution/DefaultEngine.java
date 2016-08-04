package org.stream.core.execution;

import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

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

public class DefaultEngine implements Engine {

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext,
            final String graphName,
            final boolean autoRecord,
            final ResourceType resourceType) throws WorkFlowExecutionExeception {

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
            final ResourceType resourceType) throws WorkFlowExecutionExeception {

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
            final ResourceType resourceType) throws WorkFlowExecutionExeception {

        return start(graphContext, graphName, null, autoRecord, resourceType, true);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, 
            final String graphName,
            final boolean autoRecord, 
            final ResourceType resourceType) throws WorkFlowExecutionExeception {

        return start(graphContext, graphName, null, autoRecord, resourceType, true);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reboot() {
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
            final boolean autoClean) throws WorkFlowExecutionExeception {

        Graph graph = graphContext.getGraph(graphName);
        if (graph == null) {
            throw new WorkFlowExecutionExeception("Graph is not present! Please double check the graph name you provide.");
        }

        boolean isWorkflowEntryGraph = false;

        WorkFlow workFlow;
        if (!WorkFlowContext.isThereWorkingWorkFlow()) {
            //Currently there is no working workflow in the same thread, we should create a new workflow.
            workFlow = WorkFlowContext.setUpWorkFlow();
            isWorkflowEntryGraph = true;
            workFlow.start();
            workFlow.visitGraph(graph);
            if (autoRecord) {
                ExecutionRecord record = ExecutionRecord.builder()
                        .time(workFlow.getCreateTime())
                        .description(String.format("Create a new workflow [%s] to execute the graph [%s]", workFlow.getWorkFlowName(), graphName))
                        .build();
                workFlow.keepRecord(record);
            }
        } else {
            //Current there is one workfing workflow instance attached to the thread, just reuse it.
            workFlow = WorkFlowContext.provide();
            workFlow.visitGraph(graph);
            if (autoRecord) {
                ExecutionRecord record = ExecutionRecord.builder()
                        .time(workFlow.getCreateTime())
                        .description(String.format("Add a new graph [%s] to the existed workflow [%s]", graphName, workFlow.getWorkFlowName()))
                        .build();
                workFlow.keepRecord(record);
            }
        }
        workFlow.attachPrimaryResource(resource);

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

            //Before executing the activity, we'd check if the node contains async dependency nodes, if yes, we should construct some async tasks turn back to the normal procedure.
            if (startNode.getAsyncDependencies() != null) {
                setUpAsyncTask(workFlow, startNode);
            }

            ActivityResult activityResult = startNode.perform();
            startNode = traverse(activityResult, startNode);

            if (activityResult.equals(ActivityResult.SUSPEND)) {
                /**
                 * Since the previous node return Suspend result, workflow should suspend and wait for some time to invoke the next node.
                 * Waiting time is specified by the activity himself, stored in the resource tank with a standard resource reference WAITING_TIME.
                 */
                Resource timeOut = workFlow.resolveResource(TimeOut.TIME_OUT_REFERENCE);
                Long interval = (Long) timeOut.getValue();
                try {
                    Thread.sleep(interval.longValue());
                } catch (InterruptedException e) {
                    ExecutionRecord record = ExecutionRecord.builder()
                            .time(Calendar.getInstance().getTime())
                            .description(String.format("Thread was interupted due to [%s]", e.getMessage()))
                            .build();
                    workFlow.keepRecord(record);
                }
            }
        }

        //Successful execute the logic in the current graph, remove the 
        ResourceTank resourceTank = workFlow.getResourceTank();
        if (autoClean && isWorkflowEntryGraph) {
            /**
             * Clean up the workflow. Only workflow finishs executing the work defined the entry graph(the entry point who call the execute(*) series methods). Basicclly, internal
             * graphs should call the non auto clean series methods, while the very beginning graph holder calls the auto clean series methods.
             */
            workFlow.setStatus(WorkFlowStatus.CLOSED);
            workFlow = null;
        }

        return resourceTank;
    }

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

    private void setUpAsyncTask(final WorkFlow workFlow, final Node node) {
        node.getAsyncDependencies().forEach(async -> {
            FutureTask<ActivityResult> task = new FutureTask<ActivityResult>(new Callable<ActivityResult>() {
                @Override
                public ActivityResult call() throws Exception {
                    AsyncActivity asyncActivity = (AsyncActivity) async.getActivity();
                    String primaryResourceReference = workFlow.getPrimary() == null ? null : workFlow.getPrimary().getResourceReference();
                    asyncActivity.linkUp(workFlow.getResourceTank(), primaryResourceReference);
                    return async.perform();
                }
            });
            Resource taskWrapper = Resource.builder()
                    .value(task)
                    .resourceType(ResourceType.OBJECT)
                    .resourceReference(async.getNodeName() + ResourceHelper.ASYNC_TASK_SUFFIX)
                    .build();
            workFlow.attachResource(taskWrapper);
            WorkFlowContext.submit(task);
        });
    }
}

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

import java.util.Calendar;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.exception.WorkFlowExecutionException;
import org.stream.core.execution.WorkFlow.WorkFlowStatus;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.extension.state.DefaultExecutionStateSwitcher;
import org.stream.extension.state.ExecutionStateSwitcher;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@linkplain Engine}.
 * All the work will be executed in single host and if any exception is thrown, retry procedure will not be applied.
 *
 */
@Slf4j
public class DefaultEngine implements Engine {

    private static final ThreadLocal<Integer> ENTRANCE_TAG = new ThreadLocal<>();

    static {
        ENTRANCE_TAG.set(0);
    }

    @Setter
    private ExecutionStateSwitcher executionStateSwitcher = new DefaultExecutionStateSwitcher();

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final boolean autoRecord) {
        return start(graphContext, graphName, null, autoRecord, false, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName,
            final Resource primaryResource, final boolean autoRecord) {
        return start(graphContext, graphName, primaryResource, autoRecord, false, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName,
            final Resource primaryResource, final boolean autoRecord) {
        return  start(graphContext, graphName, primaryResource, autoRecord, true, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName, final boolean autoRecord) {
        return start(graphContext, graphName, null, autoRecord, true, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeFrom(final GraphContext graphContext, final String graphName, final String startNode,
            final boolean autoRecord) {
        return start(graphContext, graphName, null, autoRecord, false, startNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeFrom(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final String startNode, final boolean autoRecord) {
        return start(graphContext, graphName, primaryResource, autoRecord, false, startNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnceFrom(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final String startNode, final boolean autoRecord) {
        return start(graphContext, graphName, primaryResource, autoRecord, true, startNode);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnceFrom(final GraphContext graphContext, final String graphName, final String startNode,
            final boolean autoRecord) {
        return start(graphContext, graphName, null, autoRecord, true, startNode);

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

    private ResourceTank start(final GraphContext graphContext, final String graphName, final Resource resource,
            final boolean autoRecord, final boolean autoClean, final String startNode) {

        // Deduce work-flow procedure definition graph.
        var graph = deduceGraph(graphName, graphContext);

        // Pre-set work-flow context flags.
        boolean isWorkflowEntryGraph = false;
        if (ENTRANCE_TAG.get() == null || ENTRANCE_TAG.get() == 0) {
            isWorkflowEntryGraph = true;
            ENTRANCE_TAG.set(1);
        }

        // Prepare work-flow runtime context.
        var context = prepare(graph, autoRecord, graphName, resource);

        // Execute
        execute(context, graph, autoRecord, startNode, graphContext);
        var resourceTank = context.getResourceTank();

        // Clean context.
        clear(context, autoClean, isWorkflowEntryGraph);

        // Return.
        return resourceTank;
    }

    private Graph deduceGraph(final String graphName, final GraphContext graphContext) {
        var graph = graphContext.getGraph(graphName);
        if (graph == null) {
            throw new WorkFlowExecutionException("Graph is not present! Please double check the graph name you provide.");
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

    private WorkFlow prepare(final Graph graph, final boolean autoRecord, final String graphName, final Resource resource) {
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

            refresh(workFlow, graph, autoRecord);
        }
        workFlow = WorkFlowContext.provide();
        workFlow.attachPrimaryResource(resource);
        // Let's turn it off, in case developers forget to clean the thread local exception after last execution.
        WorkFlowContext.markException(null);
        return workFlow;
    }

    private WorkFlow initiate(final Graph graph, final boolean autoRecord, final String graphName) {
        //Currently there is no working work-flow in the same thread, we should create a new work-flow.
        var workFlow = WorkFlowContext.setUpWorkFlow();
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

    private void refresh(final WorkFlow workFlow, final Graph graph, final boolean autoRecord) {
        if (workFlow.getStatus().equals(WorkFlowStatus.CLOSED)) {
            throw new WorkFlowExecutionException("The workflow has been closed!");
        }

        if (workFlow.getStatus().equals(WorkFlowStatus.WAITING)) {
            useCurrentWorkflowDirectly(workFlow, graph, autoRecord);
        } else {
            useCurrentWorkflowAsParentWorkflow(workFlow, graph);
        }
    }

    private void useCurrentWorkflowDirectly(final WorkFlow workFlow, final Graph graph, final boolean autoRecord) {
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
        workFlow.getAsyncTaskReferences().clear();
        workFlow.setResourceTank(new ResourceTank());
    }

    private void useCurrentWorkflowAsParentWorkflow(final WorkFlow workFlow, final Graph graph) {

        /**
         * Currently the work-flow is running for another task, we are triggered within the running work-flow context.
         * So we should be treated as sub-workflow.
         * 
         */
        var child = WorkFlowContext.setUpWorkFlow();
        child.start();
        child.setParent(workFlow);
        child.visitGraph(graph);
        workFlow.getChildren().add(child);
    }

    private void execute(final WorkFlow workFlow, final Graph graph,
            final boolean autoRecord, final String startNode,
            final GraphContext graphContext) {

        /**
         * Extract the start node of the graph, and invoke the perform() method.
         */
        Node previous = null;
        Node executionNode = graph.getStartNode();
        /**
         * Update 2020/06/03, to make it possible to start the procedure at specific start node,
         * add four methods with suffix from, which means start from the specific node.
         * Adding a new flag startNode, when the value is not null, the engines should retrieve the
         * specific node and start from that node.
         */
        if (startNode != null) {
            executionNode = graph.getNode(startNode);
            if (executionNode == null) {
                log.error("Can not find the target node [{}] from the graph [{}]", startNode, graph.getGraphName());
                throw new WorkFlowExecutionException(String.format("Start node [%s] node exists in graph [%s]",
                        startNode, graph.getGraphName()));
            }
        }
        while (executionNode != null && !WorkFlowContext.provide().isRebooting()) {

            if (isStuckInDeadLoop(executionNode, previous)) {
                WorkFlowContext.markException(new WorkFlowExecutionException("Next execution node should not be the same with the previous one."));
                break;
            }

            if (autoRecord) {
                ExecutionRecord executionRecord = ExecutionRecord.builder()
                        .time(Calendar.getInstance().getTime())
                        .description(String.format("Begin to enter the node [%s]", executionNode.getNodeName()))
                        .build();
                workFlow.keepRecord(executionRecord);
            }

            previous = executionNode;
            var activityResult = TaskHelper.perform(executionNode, ActivityResult.FAIL);

            if (ActivityResult.SUSPEND.equals(activityResult)) {
                activityResult = processSuspendCase(executionNode);
            }

            executionNode = TaskHelper.traverse(executionNode,
                    executionStateSwitcher,
                    activityResult,
                    (engine, context, graphName) -> {
                        Resource primary = WorkFlowContext.getPrimary();
                        return engine.execute(context, graphName, primary, false);
                    },
                    graphContext,
                    this);
        }
    }

    private boolean isStuckInDeadLoop(final Node next, final Node previous) {
        return next.equals(previous);
    }

    private ActivityResult processSuspendCase(final Node node) {

        /**
         * Since the previous node return Suspend result, work-flow should suspend and wait for some time to invoke the next node.
         * Waiting time is specified by the activity himself, stored in the resource tank with a standard resource reference WAITING_TIME.
         */
        if (!CollectionUtils.isEmpty(node.getIntervals())) {
            try {
                Thread.sleep(node.getIntervals().get(0));
            } catch (Exception e) {
                log.warn("Thread interrupted", e);
            }
        }

        return ActivityResult.SUCCESS;
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
            ENTRANCE_TAG.remove();
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

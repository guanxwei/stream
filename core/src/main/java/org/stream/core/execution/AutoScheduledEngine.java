package org.stream.core.execution;

import java.io.Serializable;

import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.exception.DuplicateTaskException;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.helper.Jackson;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceCatalog;
import org.stream.core.resource.ResourceTank;
import org.stream.extension.events.Event;
import org.stream.extension.events.EventCenter;
import org.stream.extension.events.EventsHelper;
import org.stream.extension.events.WorkflowInitiatedEvent;
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.StreamTransferDataStatus;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStatus;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.utils.TaskIDGenerator;
import org.stream.extension.utils.UUIDTaskIDGenerator;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Auto scheduled work-flow engine. This engine is mainly designed to support auto retry cases in distributed environments.
 * In distributed world, multiple services will be arranged to complete one task, sometimes auto retry is also needed
 * to process temporary unavailable cases. It's tricky for developers themselves to determine what to do based on the
 * predecessor, because there are so many things ahead to be done like invoking
 * next step after something is executed, retry the step if it failed in pre-set times.
 *
 * To help eliminate the effort solving such tricky problems, stream work-flow framework provides this lightly flow engine implementation.
 * With this engine, developers just need to implements their business logic in stand alone activities and managing the procedure through defining human friendly graphs.
 * Every thing else will be done silently by this engine including executing the missions in order and auto retry failed sub-missions, .etc.
 *
 * Please be aware that currently AutoScheduledEngine does not support sub-work-flow situations. If you want to run sub procedures within another
 * work-flow context, you'd probably use other tools.
 */
@Slf4j
public class AutoScheduledEngine implements Engine {

    private static final String PRIMARY_MISSING_ERROR = "Auto scheduled engine does not support cases without primary resource!";

    /**
     * Preserved resource reference for AutoScheduledEngine's primary resource.
     */
    public static final String ORIGINAL_RESOURCE_REFERENCE = "stream::scheduled::workflow::primary";

    /**
     * Task reference.
     */
    public static final String TASK_REFERENCE = "stream::autoschedule::task::reference";

    @Setter
    private int maxRetry = 10;

    @Setter
    private ResourceCatalog resourceCatalog;

    @Setter
    private TaskPersister taskPersister;

    @Setter
    private String application;

    @Setter
    private TaskExecutor taskExecutor;

    @Setter
    private TaskIDGenerator taskIDGenerator = new UUIDTaskIDGenerator();

    @Setter
    private EventCenter eventCenter;

    /**
     * Not supported in auto scheduled engine, please do not use it.
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final boolean autoRecord) {
        throw new WorkFlowExecutionExeception(PRIMARY_MISSING_ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final boolean autoRecord) {
        String taskId = start(graphName, graphContext, primaryResource.getValue(), null);
        Resource taskResource = Resource.builder()
                .value(taskId)
                .resourceReference(TASK_REFERENCE)
                .build();
        ResourceTank tank = new ResourceTank();
        tank.addResource(taskResource);
        return tank;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final boolean autoRecord) {
        return execute(graphContext, graphName, primaryResource, autoRecord);
    }

    /**
     * Not supported in auto scheduled engine, please do not use it.
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName, final boolean autoRecord) {
        throw new WorkFlowExecutionExeception(PRIMARY_MISSING_ERROR);
    }

    /**
     * Not supported in auto scheduled engine, please do not use it.
     */
    @Override
    public ResourceTank executeFrom(final GraphContext graphContext, final String graphName, final String startNode,
            final boolean autoRecord) {
        throw new WorkFlowExecutionExeception(PRIMARY_MISSING_ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeFrom(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final String startNode, final boolean autoRecord) {
        String taskId = start(graphName, graphContext, primaryResource.getValue(), startNode);
        Resource taskResource = Resource.builder()
                .value(taskId)
                .resourceReference(TASK_REFERENCE)
                .build();
        ResourceTank tank = new ResourceTank();
        tank.addResource(taskResource);
        return tank;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnceFrom(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final String startNode, final boolean autoRecord) {
        return executeFrom(graphContext, graphName, primaryResource, startNode, autoRecord);
    }

    /**
     * Not supported in auto scheduled engine, please do not use it.
     */
    @Override
    public ResourceTank executeOnceFrom(final GraphContext graphContext, final String graphName, final String startNode,
            final boolean autoRecord) {
        throw new WorkFlowExecutionExeception(PRIMARY_MISSING_ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reboot() throws InterruptedException {
        Thread.interrupted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitAndReboot() {
        Thread.interrupted();
    }

    private String start(final String graphName, final GraphContext graphContext, final Object resource,
            final String startNode) {

        if (!(resource instanceof Serializable)) {
            throw new WorkFlowExecutionExeception("Primary resource should be serializable when you are using auto schedule engine");
        }

        Resource primaryResource = Resource.builder()
                .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                .value(resource)
                .build();

        /**
         * Give a chance to the application to generate the task id according to the input primary resource
         * to implement idempotency mechanism and many other things.
         */
        String taskId = taskIDGenerator.generateTaskID(primaryResource);

        log.info("Task id [{}] assigned to the request", taskId);

        Graph graph = graphContext.getGraph(graphName);

        try {
            StreamTransferData data = new StreamTransferData();
            Task task = initiateTask(taskId, graphName, primaryResource, data, graphContext, startNode);
            EventsHelper.fireEvent(eventCenter, Event.of(WorkflowInitiatedEvent.class, task.getTaskId(),
                    graph.getStartNode()), false);
            log.info("New task [{}] initiated", task.getTaskId());
            taskExecutor.submit(primaryResource, task, data);
            log.info("Task [{}] submited", taskId);
        } catch (Exception e) {
            throw new WorkFlowExecutionExeception(e);
        }

        return taskId;
    }

    private Task initiateTask(final String taskId, final String graphName, final Resource primaryResource,
            final StreamTransferData data, final GraphContext graphContext, final String startNode) throws Exception {
        Graph graph = graphContext.getGraph(graphName);
        if (graph == null) {
            throw new WorkFlowExecutionExeception("Graph not existes! Please double check！");
        }

        if (taskPersister.get(taskId) != null) {
            throw new DuplicateTaskException();
        }

        Node firstNode = startNode == null ? graph.getStartNode() : graph.getNode(startNode);
        if (firstNode == null) {
            log.error("Can not find the target node [{}] from the graph [{}]", startNode, graph.getGraphName());
            throw new WorkFlowExecutionExeception(String.format("Start node [%s] node exists in graph [%s]",
                    startNode, graphName));
        }
        Task task = Task.builder()
                .application(application)
                .graphName(graphName)
                .initiatedTime(System.currentTimeMillis())
                .jsonfiedPrimaryResource(Jackson.json(primaryResource.getValue()))
                .lastExcutionTime(System.currentTimeMillis())
                .nextExecutionTime(System.currentTimeMillis() + 1000)
                .nodeName(firstNode.getNodeName())
                .retryTimes(0)
                .status(TaskStatus.INITIATED.code())
                .taskId(taskId)
                .build();
        data.add("primaryClass", primaryResource.getValue().getClass().getName());
        TaskStep taskStep = TaskExecutionUtils.constructStep(graph, firstNode,
                StreamTransferDataStatus.SUCCESS, data, task);
        taskPersister.initiateOrUpdateTask(task, true, taskStep);
        return task;
    }

}

package org.stream.core.execution;

import org.stream.core.component.Graph;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.helper.Jackson;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceCatalog;
import org.stream.core.resource.ResourceTank;
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.utils.TaskIDGenerator;
import org.stream.extension.utils.UUIDTaskIDGenerator;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Auto scheduled work-flow engine. This engine is mainly designed to support auto retry cases in distributed environments.
 * In distributed world, there are always more than one service arranged to complete one task, sometimes retry is also needed
 * to process temporary unavailable cases. It will be very difficult for developers to maintain the code without an efficient
 * tool to simplify the way managing the whole working procedure, because there are so many things ahead to be done like invoking
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
    private GraphContext graphContext;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final boolean autoRecord) {
        throw new WorkFlowExecutionExeception("Auto scheduled engine does not support cases without primary resource!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final boolean autoRecord) {
        String taskId = start(graphName, graphContext, primaryResource.getValue());
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
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName, final boolean autoRecord) {
        throw new WorkFlowExecutionExeception("Auto scheduled engine does not support cases without primary resource!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reboot() throws InterruptedException {
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitAndReboot() {
        return;
    }

    private String start(final String graphName, final GraphContext graphContext, final Object resource) {

        initiateContextIfNotPresent(graphContext);
        String taskId = taskIDGenerator.generateTaskID();

        log.info("Begin to process the incoming request [{}] with task id [{}]", Jackson.json(resource), taskId);

        Resource primaryResource = Resource.builder()
                .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                .value(resource)
                .build();

        Graph graph = graphContext.getGraph(graphName);

        Task task = initiateTask(taskId, graphName, primaryResource, new StreamTransferData());

        log.info("New task [{}] initiated", task.toString());
        taskExecutor.submit(graph, primaryResource, task);

        log.info("Task [{}] submited", taskId);
        return taskId;
    }

    private void initiateContextIfNotPresent(final GraphContext graphContext) {
        if (this.graphContext == null) {
            this.graphContext = graphContext;
        } else {
            if (this.graphContext != graphContext) {
                throw new WorkFlowExecutionExeception("Hi there, currently we only support"
                        + " single Graph context cases!");
            }
        }
    }

    private Task initiateTask(final String taskId, final String graphName, final Resource primaryResource,
            final StreamTransferData data) {
        Graph graph = graphContext.getGraph(graphName);
        if (graph == null) {
            throw new WorkFlowExecutionExeception("Graph not existes! Please double check！");
        }
        Task task = Task.builder()
                .application(application)
                .graphName(graphName)
                .jsonfiedPrimaryResource(primaryResource.toString())
                .lastExcutionTime(System.currentTimeMillis())
                .nextExecutionTime(System.currentTimeMillis())
                .nodeName(graph.getStartNode().getNodeName())
                .retryTimes(0)
                .status("Initiated")
                .taskId(taskId)
                .build();
        TaskStep taskStep = TaskStep.builder()
                .createTime(System.currentTimeMillis())
                .graphName(graphName)
                .nodeName(graph.getStartNode().getNodeName())
                .jsonfiedTransferData(data.toString())
                .status("Initiated")
                .taskId(taskId)
                .build();
        taskPersister.setHub(taskId, task, true, taskStep);
        return task;
    }
}

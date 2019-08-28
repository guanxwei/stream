package org.stream.core.execution;

import java.io.Serializable;

import org.stream.core.component.Graph;
import org.stream.core.exception.DuplicateTaskException;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.helper.Jackson;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceCatalog;
import org.stream.core.resource.ResourceTank;
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.io.HessianIOSerializer;
import org.stream.extension.io.StreamTransferData;
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

        if (!(resource instanceof Serializable)) {
            throw new WorkFlowExecutionExeception("Primary resource should be serializable when you are using auto schedule engine");
        }

        initiateContextIfNotPresent(graphContext);
        Resource primaryResource = Resource.builder()
                .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                .value(resource)
                .build();

        /**
         * Give a chance to the application to generate the task id according to the input primary resource
         * to implement idempotency mechanism and so on.
         */
        String taskId = taskIDGenerator.generateTaskID(primaryResource);

        log.info("Begin to process the incoming request [{}] with task id [{}]", Jackson.json(resource), taskId);

        Graph graph = graphContext.getGraph(graphName);

        try {
            StreamTransferData data = new StreamTransferData();
            Task task = initiateTask(taskId, graphName, primaryResource, data);
            log.info("New task [{}] initiated", task.toString());
            taskExecutor.submit(graph, primaryResource, task, data);
            log.info("Task [{}] submited", taskId);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new WorkFlowExecutionExeception(e);
        }

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
            final StreamTransferData data) throws Exception {
        Graph graph = graphContext.getGraph(graphName);
        if (graph == null) {
            throw new WorkFlowExecutionExeception("Graph not existes! Please double check！");
        }

        if (taskPersister.get(taskId) != null) {
            throw new DuplicateTaskException();
        }

        Task task = Task.builder()
                .application(application)
                .graphName(graphName)
                .initiatedTime(System.currentTimeMillis())
                .jsonfiedPrimaryResource(primaryResource.toString())
                .lastExcutionTime(System.currentTimeMillis())
                .nextExecutionTime(System.currentTimeMillis() + 5 * 1000)
                .nodeName(graph.getStartNode().getNodeName())
                .retryTimes(0)
                .status(TaskStatus.INITIATED.code())
                .taskId(taskId)
                .build();
        data.add("primary", (Serializable) primaryResource.getValue());
        data.add("primaryClass", primaryResource.getValue().getClass().getName());
        TaskStep taskStep = TaskStep.builder()
                .createTime(System.currentTimeMillis())
                .graphName(graphName)
                .nodeName(graph.getStartNode().getNodeName())
                .status(TaskStatus.INITIATED.type())
                .streamTransferData(HessianIOSerializer.encode(data))
                .taskId(taskId)
                .build();
        taskPersister.initiateOrUpdateTask(task, true, taskStep);
        return task;
    }
}

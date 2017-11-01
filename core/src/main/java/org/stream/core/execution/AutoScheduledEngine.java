package org.stream.core.execution;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.stream.core.component.Graph;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceCatalog;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.ResourceType;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.persist.TaskPersister;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Auto scheduled work-flow engine. This engine is mainly used to support
 * auto retry cases, extremely suitable for distributed systems.
 *
 * In distributed systems, many services will be arranged to complete one job. As the requirements changes, the system will
 * become more and more complicated, many more services will be involved. Managing the whole work-flow will turns into a big
 * problem to developers.
 *
 * To help eliminate the effort to manage the service invoke hierarchy, stream work-flow provides this lightly engine implementation.
 * Developers just need to define their own procedure graph, this engine will automatically pick the graph and help execute the whole task.
 * The most important difference between this engine and {@link DefaultEngine} is that the works in each node is to communicate with
 * external services, and if the engine fail to connect the services, it will help to retry in the near future.
 *
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

    /**
     * Work-flow retry pattern.
     * "EqualDifference" : Will retry the stuck work-flow at fixed rate.
     * "ScheduledTime" : Will retry the stuck work-flow at the scheduled time.
     *
     * "EqualDifference" : default option.
     */
    @Setter
    private String pattern = RetryPattern.EQUAL_DIFFERENCE;

    private ExecutorService executorService = Executors.newFixedThreadPool(100);

    /**
     * Begin to execute auto scheduled job according to graph definition file.
     *
     * @param graphContext Graph context.
     * @param graphName Graph's name to be executed on.
     * @param autoRecord Ignore it.
     * @param resourceType Ignore it.
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final boolean autoRecord,
            final ResourceType resourceType) {
        String taskId = start(graphName, graphContext, null);
        Resource taskResource = Resource.builder()
                .value(taskId)
                .resourceReference(TASK_REFERENCE)
                .build();
        ResourceTank tank = new ResourceTank();
        tank.addResource(taskResource);
        return tank;
    }

    /**
     * Begin to execute auto scheduled job according to graph definition file.
     *
     * @param graphContext Graph context.
     * @param graphName Graph's name to be executed on.
     * @param autoRecord Ignore it.
     * @param primaryResource Resource to be used in the execution phase.
     * @param resourceType Ignore it.
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final boolean autoRecord, final ResourceType resourceType) {
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
     * Begin to execute auto scheduled job according to graph definition file.
     *
     * @param graphContext Graph context.
     * @param graphName Graph's name to be executed on.
     * @param autoRecord Ignore it.
     * @param primaryResource Resource to be used in the execution phase.
     * @param resourceType Ignore it.
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final boolean autoRecord, final ResourceType resourceType) {
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
     * Begin to execute auto scheduled job according to graph definition file.
     *
     * @param graphContext Graph context.
     * @param graphName Graph's name to be executed on.
     * @param autoRecord Ignore it.
     * @param resourceType Ignore it.
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName, final boolean autoRecord,
            final ResourceType resourceType) {
        String taskId = start(graphName, graphContext, null);
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
        String taskId = UUID.randomUUID().toString();

        log.info("Begin to process the incoming request with task id [{}]", taskId);

        StreamTransferData data = new StreamTransferData();
        data.set(ORIGINAL_RESOURCE_REFERENCE, resource);
        Resource primaryResource = Resource.builder()
                .resourceReference("Auto_Scheduled_Workflow_PrimaryResource_Reference")
                .value(data)
                .build();
        Task task = initiateTask(taskId, graphName, primaryResource, data);
        ExecutionRunner runner = new ExecutionRunner(graphContext, graphName, pattern, primaryResource, task, taskPersister);
        executorService.submit(runner);
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
                .taskId(taskId)
                .graphName(graphName)
                .lastExcutionTime(System.currentTimeMillis())
                .nodeName(graph.getStartNode().getNodeName())
                .jsonfiedPrimaryResource(primaryResource.toString())
                .jsonfiedTransferData(data.toString())
                .status("Initiated")
                .build();
        taskPersister.tryLock(taskId);
        taskPersister.setHub(taskId, task.toString());
        return task;
    }

    /**
     * Initiation method to prepare back-end workers to process pending on retry work-flow instances.
     */
    public void init() {
        Runnable backend = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    /**
                     * Updated 2017/09/30:
                     * We decided to support two strategies to re-run the suspended work-flow instances:
                     * 1、Re-run the suspended instances periodically with up-limit times 12. That's what we do here.
                     * 2、Re-run the suspended instances at fixed time points, currently we will re-run the suspended work-flow at most
                     *     12 times, the time points are:5s,10s,30s,60s,5m,10m,30m,1h,3h,10h,20h,24h,this strategy depends on Redis's zadd/zrange
                     *     function, each time when we mark the work-flow as suspended, we also set up the expire time as the score for the task,
                     *     before re-runing the work-flow we will check if the time fulfill our requirements.
                     *
                     * Fetch suspended cases.
                     *
                     */
                    List<String> contents = new LinkedList<>();
                    contents.addAll(taskPersister.getPendingList(1));
                    // Then fetch crashed cases.
                    contents.addAll(taskPersister.getPendingList(2));
                    if (!contents.isEmpty()) {
                        process(contents);
                    }
                    // The stuck work-flows should be re-ran at least 5 seconds later, so we can sleep here for 5 minutes safely.
                    sleep(5000);
                }
            }
        };
        executorService.submit(backend);
    }

    private void process(final List<String> contents) {
        for (String content :contents) {
            RetryRunner worker = new RetryRunner(taskPersister.get(content), graphContext, taskPersister, pattern);
            executorService.submit(worker);
        }
    }

    private void sleep(final long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

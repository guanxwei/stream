package org.stream.core.execution;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.FutureTask;

import org.stream.core.component.Graph;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.helper.ResourceHelper;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Encapsulation of an working flow instance reserving the work-flow meta-data. Clients can check the work-flow status at any time.
 * The work-flow is created when a work-flow engine is invoked to execute a graph, while the current thread does not have other working
 * work-flow instances.
 */
public class WorkFlow {

    @Getter @Setter
    private String workFlowId;

    private List<ExecutionRecord> records = new LinkedList<ExecutionRecord>();

    @Setter @Getter
    private boolean isRebooting = false;

    @Setter @Getter
    private GraphContext graphContext;

    /**
     * The primary resource the work-flow works on. Basically it is specified by the first graph invoker, and will keep immutable.
     */
    @Setter
    private String primaryResourceReference;

    /**
     * Resource tank used to store resources attached to the work-flow. Nodes in the current work-flow share the resource tank.
     */
    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    private ResourceTank resourceTank;

    /**
     * Work-flow name, should be specified by the clients. The stream framework will not use it, but clients may use it.
     */
    @Getter @Setter
    private String workFlowName;

    /**
     * The time work-flow is created.
     */
    @Setter
    @Getter
    private Date createTime;

    /**
     * The work-flow status.
     */
    @Getter @Setter
    private WorkFlowStatus status;

    /**
     * Exception happened during execution.
     */
    @Getter
    private Exception e;

    /**
     * The graph instances this work-flow have been executing on. Every time clients invoke the {@linkplain Engine} to execute a graph, the graph reference will be added to the work-flow.
     */
    private Map<String, Graph> graphs;

    /**
     * The references to the async tasks submitted by this work-flow instance.
     */
    @Getter(value = AccessLevel.PROTECTED)
    private List<String> asyncTaksReferences;

    /**
     * Default constructor.
     */
    public WorkFlow() {
        this.workFlowId = UUID.randomUUID().toString();
        this.status = WorkFlowStatus.WAITING;
        this.graphs = new HashMap<>();
        this.asyncTaksReferences = new LinkedList<>();
        resourceTank = new ResourceTank();
    }

    /**
     * Force the work-flow status to {@link WorkFlowStatus#WORKING}.
     */
    protected void start() {
        this.status = WorkFlowStatus.WORKING;
    }

    /**
     * Force the work-flow status to {@linkplain WorkFlowStatus#CLOSED}.
     */
    protected void close() {
        this.status = WorkFlowStatus.CLOSED;
    }

    /**
     * Add a execution record to the ledger.
     * @param record
     */
    protected void keepRecord(ExecutionRecord record) {
        records.add(record);
    }

    protected List<ExecutionRecord> getRecords() {
        return records;
    }

    /**
     * Attach a resource to the work-flow.
     * @param resource
     */
    protected void attachResource(final Resource resource) {
        resourceTank.addResource(resource);
    }

    /**
     * Extract a resource object from the resource tank.
     * @param resourceReference
     * @return
     */
    protected Resource resolveResource(final String resourceReference) {
        return resourceTank.resolve(resourceReference);
    }

    /**
     * Add a new graph to the work-flow, the work-flow will handle it sooner.
     * @param graph
     */
    protected void visitGraph(Graph graph) {
        graphs.put(graph.getGraphName(), graph);
    }

    /**
     * Attach a primary source to the work-flow, once appointed, the primary resource should never be changed.
     * @param resource
     * @throws WorkFlowExecutionExeception 
     */
    protected void attachPrimaryResource(final Resource resource) throws WorkFlowExecutionExeception {

        if (resource == null) {
            return;
        }

        if (primaryResourceReference == null) {
            primaryResourceReference = resource.getResourceReference();
            attachResource(resource);
        } else {
            throw new WorkFlowExecutionExeception("Attempt to change primary resource!");
        }

    }

    /**
     * Extract the primary resource of the workflow.
     * @return
     */
    protected Resource getPrimary() {
        if (primaryResourceReference == null) {
            return null;
        } else {
            return resolveResource(primaryResourceReference);
        }
    };

    /**
     * Get async task wrapper. Value contained in the wrapper is an instance of {@link FutureTask} which returns the execution result of async activity.
     * @param nodeName The node name of async activity node.
     * @return async task wrapper instance.
     */
    protected Resource getAsyncTaskWrapper(final String nodeName) {
        return resolveResource(nodeName + ResourceHelper.ASYNC_TASK_SUFFIX);
    }

    /**
     * Mark the exception that cause the system ran into crash. Only used when the system itself can not tune to normal from the exception. Basically, this method should
     * be used only once for every single execution plan. After the work-flow engine handle over the control to the invoker, the invoker can check if the {{@link #e} is
     * null, if not they can log the exception message to the log by there own strategy.
     * @param e exception that cause the work-flow ran into crash.
     */
    protected void markException(final Exception e) {
        this.e = e;
    }

    protected void addAsyncTasks(final String taskReference) {
        this.asyncTaksReferences.add(taskReference);
    }

    public enum WorkFlowStatus {

        WAITING(1), WORKING(2), CLOSED(3);

        private int status;

        private WorkFlowStatus(int status) {
            this.status = status;
        };

        public int getStatusCode() {
            return status;
        }
    }

}

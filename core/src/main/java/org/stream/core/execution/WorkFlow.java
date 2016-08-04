package org.stream.core.execution;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.stream.core.component.Graph;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.helper.ResourceHelper;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Encapsulation of an working flow instance reserving the workflow metadata. Clients can check the workflow status at any time.
 * The workflow is created when a workflow engine is invoked to execute a graph, while the current thread does not have other working
 * workflow instances.
 */
public class WorkFlow {

    private List<ExecutionRecord> records = new LinkedList<ExecutionRecord>();

    @Setter @Getter
    private boolean isRebooting = false;

    @Setter @Getter
    private GraphContext graphContext;

    /**
     * The primary resoure the workflow works on. Basically it is sepcified by the first graph invoker, and will keep immutable.
     */
    @Setter
    private String primaryResourceReference;

    /**
     * Resource tank used to store resources attached to the workflow. Nodes in the current workflow share the resource tank.
     */
    @Getter(value = AccessLevel.PACKAGE)
    private ResourceTank resourceTank;

    /**
     * Workflow name, should be specified by the clients. The stream framework will not use it, but clients may use it.
     */
    @Getter @Setter
    private String workFlowName;

    /**
     * The time workflow is created.
     */
    @Setter
    @Getter
    private Date createTime;

    /**
     * The workflow status.
     */
    @Getter @Setter
    private WorkFlowStatus status;

    /**
     * The graph instances this workflow have been executing on. Every time cliens invoke the {@linkplain Engine} to execute a graph, the graph reference will be added to the workflow.
     */
    private List<Graph> graphs;

    /**
     * Default constructor.
     */
    public WorkFlow() {
        this.status = WorkFlowStatus.WAITING;
        this.graphs = new LinkedList<Graph>();
        resourceTank = new ResourceTank();
    }

    /**
     * Force the workflow status to {@link WorkFlowStatus#WORKING}.
     */
    protected void start() {
        this.status = WorkFlowStatus.WORKING;
    }

    /**
     * Force the workflow status to {@linkplain WorkFlowStatus#CLOSED}.
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
     * Attach a resource to the workflow.
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
     * Add a new graph to the workflow, the workflow will handle it sooner.
     * @param graph
     */
    protected void visitGraph(Graph graph) {
        graphs.add(graph);
    }

    /**
     * Appoint a primary source to the workflow, once appointed, the primary resource should never be changed.
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

    protected Resource getAsyncTaskWrapper(final String nodeName) {
        return resolveResource(nodeName + ResourceHelper.ASYNC_TASK_SUFFIX);
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

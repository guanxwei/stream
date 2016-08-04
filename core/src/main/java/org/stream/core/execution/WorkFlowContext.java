package org.stream.core.execution;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.WorkFlow.WorkFlowStatus;
import org.stream.core.resource.Resource;

/**
 * Workflow helper class. Help to create a new workflow, provide existed workflow, reboot the workflow.
 */
public final class WorkFlowContext {

    private static final ThreadLocal<WorkFlow> current = new ThreadLocal<WorkFlow>();

    private static final ExecutorService executorServiceForAsyncTasks = Executors.newFixedThreadPool(10);

    /**
     * Check if there is working workflow in the current thread context. We'd make sure that each thread has only one working workflow instance.
     * @return
     */
    public static boolean isThereWorkingWorkFlow() {
        return current.get() != null;
    }

    /**
     * Set up a new workflow instance for current thread. The new created workflow instance's status will be {@link WorkFlowStatus#WAITING}.
     * Clients should manually start the workflow by invoking the method {@link WorkFlow#start()}, then the workflow will retrive the graph and
     * execute the logic defined in the graph definition file.
     * @return
     */
    public static WorkFlow setUpWorkFlow() {
        WorkFlow newWorkFlow = new WorkFlow();
        Date createTime = Calendar.getInstance().getTime();
        newWorkFlow.setCreateTime(createTime);
        current.set(newWorkFlow);
        return newWorkFlow;
    }

    /**
     * Provide the current working workflow reference.
     * @return
     */
    public static WorkFlow provide() {
        return current.get();
    }

    /**
     * Reboot the workflow, it needs lubrication!
     */
    public static void reboot() {
        current.set(null);
    }

    /**
     * Get the task wrapper defined by the node having the nodeName.
     * @param nodeName
     * @return
     */
    public static Resource getAsyncTaskWrapper(final String nodeName) {
        return current.get().getAsyncTaskWrapper(nodeName);
    }

    /**
     * Get all the execution records recored during the procedure.
     * @return
     */
    public static List<ExecutionRecord> getRecords() {
        return current.get().getRecords();
    }

    /**
     * Force the workflow status to {@linkplain WorkFlowStatus#CLOSED}.
     */
    public static void close() {
        current.get().setStatus(WorkFlowStatus.CLOSED);
    }

    /**
     * Add a execution record to the ledger.
     * @param record
     */
    public static void keepRecord(ExecutionRecord record) {
        current.get().keepRecord(record);
    }

    /**
     * Attach a resource to the workflow.
     * @param resource
     */
    public static void attachResource(final Resource resource) {
        current.get().attachResource(resource);
    }

    /**
     * Extract a resource object from the resource tank.
     * @param resourceReference
     * @return
     */
    public static Resource resolveResource(final String resourceReference) {
        return current.get().resolveResource(resourceReference);
    }

    /**
     * Add a new graph to the workflow, the workflow will handle it sooner.
     * @param graph
     */
    public static void visitGraph(Graph graph) {
        current.get().visitGraph(graph);
    }

    /**
     * Appoint a primary source to the workflow, once appointed, the primary resource should never be changed.
     * @param resource
     * @throws WorkFlowExecutionExeception 
     */
    public static void attachPrimaryResource(final Resource resource) throws WorkFlowExecutionExeception {

        if (resource == null) {
            return;
        }

        if (current.get().getPrimary() == null) {
            current.get().setPrimaryResourceReference(resource.getResourceReference());
            attachResource(resource);
        } else {
            throw new WorkFlowExecutionExeception("Attempt to change primary resource!");
        }

    }

    /**
     * Extract the primary resource of the workflow.
     * @return
     */
    public static Resource getPrimary() {
        return current.get().getPrimary();
    };

    public static void submit(FutureTask<ActivityResult> task) {
        executorServiceForAsyncTasks.submit(task);
    }

}

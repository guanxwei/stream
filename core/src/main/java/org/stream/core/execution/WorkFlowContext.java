package org.stream.core.execution;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.WorkFlow.WorkFlowStatus;
import org.stream.core.resource.Resource;

/**
 * Work-flow helper class. Help to create a new work-flow or provide existed work-flow, or even reboot the work-flow.
 * Also provide helper method to retrieve objects from the thread related work-flow.
 */
public final class WorkFlowContext {

    private static final ThreadLocal<WorkFlow> CURRENT = new ThreadLocal<WorkFlow>();

    private static final ConcurrentHashMap<String, WorkFlow> WORKFLOWS = new ConcurrentHashMap<>();

    private static final ExecutorService EXECUTOR_SERVICE_FOR_ASYNC_TASKS = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * Check if there is working work-flow in the current thread context. We'd make sure that each thread has only one working work-flow instance.
     * @return
     */
    public static boolean isThereWorkingWorkFlow() {
        return CURRENT.get() != null;
    }

    /**
     * Set up a new work-flow instance for the current thread. The new created work-flow instance's status will be {@link WorkFlowStatus#WAITING}.
     * Clients should manually start the work-flow by invoking the method {@link WorkFlow#start()}, default the {@linkplain Engine} will help
     * to invoke this method when create new work-flow instance.
     * 
     * Users should not invoke this method in any cases.
     * @return The work-flow reference.
     */
    protected static WorkFlow setUpWorkFlow() {
        WorkFlow newWorkFlow = new WorkFlow();
        Date createTime = Calendar.getInstance().getTime();
        newWorkFlow.setCreateTime(createTime);
        WORKFLOWS.put(newWorkFlow.getWorkFlowId(), newWorkFlow);
        CURRENT.set(newWorkFlow);
        return newWorkFlow;
    }

    /**
     * Provide the current working work-flow reference.
     * @return
     */
    protected static WorkFlow provide() {
        return CURRENT.get();
    }

    /**
     * Reboot the work-flow, it needs lubrication!
     */
    public static void reboot() {
        close(true);
        CURRENT.set(null);
    }

    /**
     * Get the task wrapper defined by the node having the nodeName.
     * @param nodeName
     * @return
     */
    public static Resource getAsyncTaskWrapper(final String nodeName) {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        return CURRENT.get().getAsyncTaskWrapper(nodeName);
    }

    /**
     * Get all the execution records recored during the procedure.
     * @return
     */
    public static List<ExecutionRecord> getRecords() {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        return CURRENT.get().getRecords();
    }

    /**
     * Force the work-flow status to {@linkplain WorkFlowStatus#CLOSED}. And shut down the back-end running async tasks.
     * @param waitTermination parameter to indicate if we need to wait until all the async tasks are shutdown.
     */
    public static void close(final boolean mayInterruptIfRunning) {
        WorkFlow current = CURRENT.get();
        List<String> asyncTasks = current.getAsyncTaksReferences();
        asyncTasks.forEach(task -> {
            Resource taskWrapper = CURRENT.get().resolveResource(task);
            @SuppressWarnings("unchecked")
            FutureTask<ActivityResult> future = (FutureTask<ActivityResult>) taskWrapper.getValue();
            if (!future.isDone() && !future.isCancelled()) {
                future.cancel(mayInterruptIfRunning);
            }
        });
        CURRENT.get().setStatus(WorkFlowStatus.CLOSED);
    }

    /**
     * Add a execution record to the ledger.
     * @param record
     */
    public static void keepRecord(ExecutionRecord record) {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        CURRENT.get().keepRecord(record);
    }

    /**
     * Attach a resource to the work-flow.
     * @param resource
     */
    public static void attachResource(final Resource resource) {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        CURRENT.get().attachResource(resource);
    }

    /**
     * Extract a resource object from the resource tank.
     * @param resourceReference
     * @return
     */
    public static Resource resolveResource(final String resourceReference) {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        return CURRENT.get().resolveResource(resourceReference);
    }

    /**
     * Add a new graph to the work-flow, the work-flow will handle it sooner.
     * @param graph
     */
    public static void visitGraph(Graph graph) {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        CURRENT.get().visitGraph(graph);
    }

    /**
     * Appoint a primary source to the work-flow, once appointed, the primary resource should never be changed.
     * @param resource The primary resource being attached.
     * @throws WorkFlowExecutionExeception Exception thrown during execution
     */
    public static void attachPrimaryResource(final Resource resource) throws WorkFlowExecutionExeception {

        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        if (resource == null) {
            return;
        }

        if (CURRENT.get().getPrimary() == null) {
            CURRENT.get().setPrimaryResourceReference(resource.getResourceReference());
            attachResource(resource);
        } else {
            throw new WorkFlowExecutionExeception("Attempt to change primary resource!");
        }

    }

    /**
     * Extract the primary resource of the work-flow.
     * @return
     */
    public static Resource getPrimary() {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        return CURRENT.get().getPrimary();
    };

    /**
     * Submit an async task to the executor.
     * @param task Async task.
     */
    public static void submit(FutureTask<ActivityResult> task) {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        EXECUTOR_SERVICE_FOR_ASYNC_TASKS.submit(task);
    }

    /**
     * Mark that there are exceptions occur during execution.
     * @param e
     */
    public static void markException(Exception e) {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        CURRENT.get().markException(e);
    }

    /**
     * Extract the root cause exception.
     * @return
     */
    public static Exception extractException() {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception("The work-flow instance has been closed!");
        }
        return CURRENT.get().getE();
    }
}

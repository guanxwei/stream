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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.WorkFlow.WorkFlowStatus;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceURL;
import org.stream.extension.io.StreamTransferData;

import com.mongodb.annotations.ThreadSafe;

/**
 * Encapsulation of work-flow execution context.
 *
 * A work-flow context is mainly used to create a new work-flow, provide existed work-flow, or even reboot the work-flow.
 * Work-flow context also provides many convenient methods to manage the resources attached to the work-flow.
 * The work-flow context is the only bridge between the user's code and the work-flow instance.
 *
 * Each thread has it's own context attached to one work-flow with its
 * child sub work-flows, so the methods in this class are thread safe.
 */
@ThreadSafe
public final class WorkFlowContext {

    private WorkFlowContext() { }

    // The thread specific work-flow instance.
    private static final ThreadLocal<WorkFlow> CURRENT = new ThreadLocal<>();

    // All the live work-flow instances in the JVM.
    private static final ConcurrentHashMap<String, WorkFlow> WORKFLOWS = new ConcurrentHashMap<>();

    // Asynchronous task execution pool.
    private static final ExecutorService EXECUTOR_SERVICE_FOR_ASYNC_TASKS;

    private static final String STREAM_POOL_SIZE = "stream.async.pool.size";
    private static final String WORK_FLOW_CLOSE_ERROR_MESSAGE = "The work-flow instance has been closed!";

    // In case users want to define the pool size according to requirement.
    static {
        if (System.getProperty(STREAM_POOL_SIZE) == null) {
            EXECUTOR_SERVICE_FOR_ASYNC_TASKS = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2,
                    Runtime.getRuntime().availableProcessors() * 2, 0l, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(200));
        } else {
            EXECUTOR_SERVICE_FOR_ASYNC_TASKS = new ThreadPoolExecutor(Integer.parseInt(System.getProperty(STREAM_POOL_SIZE)),
                    Integer.parseInt(System.getProperty(STREAM_POOL_SIZE)), 0l, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(200));
        }
    }

    /**
     * A pre-defined work-flow resource reference to response code.
     * The value of the response code resource should always be {@link Integer}.
     */
    public static final String WORK_FLOW_RESPONSE_CODE_REFERENCE = "Stream::Workflow::ResponseCode::Reference";

    /**
     * A pre-defined work-flow resource reference to the response entity.
     * The response entity could be any of user specific types.
     */
    public static final String WORK_FLOW_RESPONSE_REFERENCE = "Stream::Workflow::Response::Reference";

    /**
     * A pre-defined work-flow resource reference to the transfer data.
     * The type of the value of referenced resource is {@link StreamTransferData}.
     */
    public static final String WORK_FLOW_TRANSTER_DATA_REFERENCE = "Stream::Workflow::Transfer::Data::Reference";

    /**
     * Check if there is working work-flow in the current thread context.
     * We'd make sure that each thread has only one working work-flow instance, since version 0.1.6 we have supported
     * sub-work-flow normally, so there would be more than one working work-flow instance within the same thread, but there
     * is only one instance at the top of work-flow hierarchy, other work-flow instances will be treated as the top instance's
     * descendants.
     *
     * @return Checking result.
     */
    public static boolean isThereWorkingWorkFlow() {
        return CURRENT.get() != null;
    }

    /**
     * Set up a new work-flow instance for the current thread. The new created work-flow instance's status will be {@link WorkFlowStatus#WAITING}.
     * Clients should manually start the work-flow by invoking the method {@link WorkFlow#start()}, normally the {@linkplain Engine} implementation will help
     * invoke this method when create a new work-flow instance.
     *
     * Users should not invoke this method in any cases.
     * @return The work-flow reference.
     */
    protected static WorkFlow setUpWorkFlow() {
        WorkFlow newWorkFlow = new WorkFlow();
        newWorkFlow.setChildren(new LinkedList<>());
        Date createTime = Calendar.getInstance().getTime();
        newWorkFlow.setCreateTime(createTime);
        WORKFLOWS.put(newWorkFlow.getWorkFlowId(), newWorkFlow);
        CURRENT.set(newWorkFlow);
        return newWorkFlow;
    }

    /**
     * Provide the current working work-flow reference.
     * @return The work-flow instance adhered to the current thread.
     */
    protected static WorkFlow provide() {
        return CURRENT.get();
    }

    /**
     * Reboot the work-flow, it needs lubrication!
     */
    public static void reboot() {
        close(true);
        WORKFLOWS.remove(CURRENT.get().getWorkFlowId());
        CURRENT.get().getRecords().clear();
        WorkFlow parent = CURRENT.get().getParent();
        // Hand over responsibility to the father instance. If there is no parent instance, exit directly.
        CURRENT.set(parent);
    }

    /**
     * Get the task wrapper defined by the node having the nodeName.
     * @param nodeName The asynchronous task's node name.
     * @return Asynchronous task wrapper.
     */
    public static Resource getAsyncTaskWrapper(final String nodeName) {
        assertWorkFlowNotClose();

        return CURRENT.get().getAsyncTaskWrapper(nodeName);
    }

    /**
     * Get all the execution records generated during the execution procedure.
     * @return ExecutionRecord list.
     */
    public static List<ExecutionRecord> getRecords() {
        assertWorkFlowNotClose();

        return CURRENT.get().getRecords();
    }

    /**
     * Force the work-flow status changing to {@linkplain WorkFlowStatus#CLOSED}. And shut down the back-end running asynchronous tasks.
     * @param mayInterruptIfRunning Parameter to indicate if we need to wait until all the asynchronous tasks are shutdown.
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
     * @param record ExecutionRecord to be recorded.
     */
    public static void keepRecord(final ExecutionRecord record) {
        assertWorkFlowNotClose();

        CURRENT.get().keepRecord(record);
    }

    /**
     * Attach a resource to the work-flow.
     * @param resource Resource to be attached.
     */
    public static void attachResource(final Resource resource) {
        assertWorkFlowNotClose();

        Resource primary = getPrimary();
        if (primary != null
                && StringUtils.equals(resource.getResourceReference(), primary.getResourceReference())) {
            throw new WorkFlowExecutionExeception("Attempt to change primary resource!"); 
        }

        if (primary != null && resource.getResourceURL() != null && primary.getResourceURL() != null
                && StringUtils.equals(primary.getResourceURL().getPath(), resource.getResourceURL().getPath())) {
            throw new WorkFlowExecutionExeception("Attempt to change primary resource!");
        }

        CURRENT.get().attachResource(resource);
    }

    /**
     * Extract a resource object from the resource tank.
     * @param resourceReference Resource reference.
     * @return Resource corresponding to the reference.
     */
    public static Resource resolveResource(final String resourceReference) {
        assertWorkFlowNotClose();

        return CURRENT.get().resolveResource(resourceReference);
    }

    /**
     * Extract a resource object from the resource tank.
     * @param url Resource url.
     * @return Resource corresponding to the reference.
     */
    public static Resource resolveResource(final ResourceURL url) {
        assertWorkFlowNotClose();

        return CURRENT.get().resolveResource(url);
    }

    /**
     * Remove the resource.
     * @param resourceReference A reference to the resource.
     */
    public static void remove(final String resourceReference) {
        assertWorkFlowNotClose();
        CURRENT.get().getResourceTank().remove(resourceReference);
    }

    /**
     * Add a new graph to the work-flow, the work-flow will handle it sooner.
     * @param graph Graph to be visited.
     */
    public static void visitGraph(final Graph graph) {
        assertWorkFlowNotClose();

        CURRENT.get().visitGraph(graph);
    }

    /**
     * Appoint a primary source to the work-flow, once appointed, the primary resource should never be changed.
     * @param resource The primary resource being attached.
     * @throws WorkFlowExecutionExeception Exception thrown during execution
     */
    public static void attachPrimaryResource(final Resource resource) throws WorkFlowExecutionExeception {
        assertWorkFlowNotClose();

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
     * @return Primary resource.
     */
    public static Resource getPrimary() {
        assertWorkFlowNotClose();

        return CURRENT.get().getPrimary();
    }

    /**
     * Submit an async task to the executor.
     * @param task Async task.
     */
    public static void submit(final FutureTask<ActivityResult> task) {
        assertWorkFlowNotClose();

        EXECUTOR_SERVICE_FOR_ASYNC_TASKS.submit(task);
    }

    /**
     * Mark that there are exceptions occur during execution.
     * @param e Exception.
     */
    public static void markException(final Exception e) {
        assertWorkFlowNotClose();

        CURRENT.get().markException(e);
    }

    /**
     * Extract the root cause exception.
     * @return The root exception caused any issues.
     */
    public static Exception extractException() {
        assertWorkFlowNotClose();
        return CURRENT.get().getE();
    }

    /**
     * Resolve resource containing response code.
     * @return Resource containing response code.
     */
    public static Resource resolveResponseCodeResource() {
        return resolveResource(WORK_FLOW_RESPONSE_CODE_REFERENCE);
    }

    /**
     * Resolve response resource.
     * @return Resource containing response entity.
     */
    public static Resource resolveResponseResource() {
        return resolveResource(WORK_FLOW_RESPONSE_REFERENCE);
    }

    /**
     * Resolve transfer data resource. Used in auto scheduled engine context only.
     * @return Transfer data resource.
     */
    public static Resource resolveTransferDataResource() {
        return resolveResource(WORK_FLOW_TRANSTER_DATA_REFERENCE);
    }

    /**
     * Helper method to resolve resource value directly from the work flow context.
     * @param reference Resource reference.
     * @param clazz Resource's real type.
     * @param <T> target class.
     * @return The value of the target resource.
     */
    public static <T> T resolve(final String reference, final Class<T> clazz) {
        Resource resource = resolveResource(reference);

        return resource.resolveValue(clazz);
    }

    private static void assertWorkFlowNotClose() {
        if (CURRENT.get().getStatus() == WorkFlowStatus.CLOSED) {
            throw new WorkFlowExecutionExeception(WORK_FLOW_CLOSE_ERROR_MESSAGE);
        }
    }
}

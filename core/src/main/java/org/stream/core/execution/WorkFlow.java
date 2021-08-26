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
 * Encapsulation of a work procedure. A work-flow typically represents a complete working procedure that
 * should be done as a unit, like a transaction in RDS world.
 *
 * Basically work-flow is created when a work-flow {@link Engine} is invoked to execute a graph,
 * while the current thread does not have an existing work-flow instance. So the work-flows will be managed by the {@link Engine}s,
 * users' code do not have to manage the life cycle of the work-flows, moreover work-flow is transparent to users' code.
 * If users want to communicate with other code in the same work-flow, for example store something in the work-flow context
 * so that they can be reused in the near future, they can invoke the methods provided by {@link WorkFlowContext},
 * like {@link WorkFlowContext#attachResource(Resource)} then {@link WorkFlowContext} will help store the resources in the proper
 * repositories and make sure that other code can reuse these resources.
 *
 * For the cases that sub-procedures are needed, {@link WorkFlow} provides a mechanism to support that, sub-procedures within
 * an existing work-flow will be treated as it's children work-flows, the requests like resolve resource attached in the context
 * will be delegated to the parent work-flow first then the current work-flow if they are not found. Sub work-flows will be unloaded
 * once the sub procedure is done, the related resource attached in the sub work-flow will be cleaned too.
 *
 * Each work-flow has its unique primary resource and can not be changed once set. A primary resource is set at the time
 * applications execute a graph through an {@link Engine}, for detail please refer to {@link Engine#execute(GraphContext, String, Resource, boolean)}
 * or {@link Engine#executeOnce(GraphContext, String, Resource, boolean)}. The engines will help attached the primary resource to the
 * work-flow instance the engine is using, the other code can visit the primary resource via calling {@link WorkFlowContext#getPrimary()}.
 * So in most cases, the value of the primary resource should be the "request" or something like that who triggers the execution
 * of the work-flow.
 *
 * After execution an instance of {@link ResourceTank} will be returned to the clients, this object contains all the resources
 * attached to the work-flow, so if your want to check if something has happened, you can check if a resource is attached to
 * the work-flow and the state of the resource fulfills your requirement.
 */
public class WorkFlow {

    @Getter @Setter
    private String workFlowId;

    private List<ExecutionRecord> records = new LinkedList<>();

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
     * The parent work-flow instance.
     */
    @Setter @Getter
    private WorkFlow parent;

    /**
     * The children work-flow instances.
     */
    @Setter @Getter
    private List<WorkFlow> children;

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
     * The graph instances this work-flow have been executing on.
     * Every time clients invoke the {@linkplain Engine} to execute a graph,
     * the graph reference will be added to the work-flow.
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
     * @param record ExecutionRecord to be recorded.
     */
    protected void keepRecord(final ExecutionRecord record) {
        records.add(record);
    }

    /**
     * Get execution record list.
     * @return ExecutionRecord list.
     */
    protected List<ExecutionRecord> getRecords() {
        return records;
    }

    /**
     * Attach a resource to the work-flow.
     * @param resource Resource to be attached.
     */
    protected void attachResource(final Resource resource) {
        resourceTank.addResource(resource);
    }

    /**
     * Extract a resource object from the resource tank.
     * @param resourceReference The resource's reference.
     * @return Resource instance corresponding to the reference.
     */
    protected Resource resolveResource(final String resourceReference) {
        Resource resource = resourceTank.resolve(resourceReference);
        if (resource == null && parent != null) {
            resource = parent.resolveResource(resourceReference);
        }

        return resource;
    }

    /**
     * Add a new graph to the work-flow, the work-flow will handle it sooner.
     * @param graph Graph to be visited
     */
    protected void visitGraph(final Graph graph) {
        graphs.put(graph.getGraphName(), graph);
    }

    /**
     * Attach a primary source to the work-flow, once appointed, the primary resource should never be changed.
     * @param resource resource Resource to be attached.
     * @throws WorkFlowExecutionExeception WorkFlowExecutionExeception
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
     * Extract the primary resource of the work-flow.
     * @return The primary resource of the work-flow.
     */
    protected Resource getPrimary() {
        if (primaryResourceReference == null) {
            if (parent != null) {
                return parent.getPrimary();
            }
            return null;
        } else {
            return resolveResource(primaryResourceReference);
        }
    }

    /**
     * Get asynchronous task wrapper. Value contained in the wrapper is an instance of {@link FutureTask}
     * which provides the execution result of asynchronous activity.
     *
     * @param nodeName The node name of asynchronous activity node.
     * @return asynchronous task wrapper instance.
     */
    protected Resource getAsyncTaskWrapper(final String nodeName) {
        return resolveResource(nodeName + ResourceHelper.ASYNC_TASK_SUFFIX);
    }

    /**
     * Mark the exception that cause the system ran into crash.
     * Only used when the system itself can not tune to normal state from the exception.
     * Basically, this method should be used only once for every single execution plan.
     * After the work-flow engine handle over the control to the invoker, the invoker can check if the {@link #e} is
     * null, if not they can log the exception message to the log by there own strategy.
     * @param e exception that cause the work-flow ran into crash.
     */
    protected void markException(final Exception e) {
        this.e = e;
    }

    /**
     * Add an asynchronous task for the current work-flow.
     * @param taskReference Reference to the task.
     */
    protected void addAsyncTasks(final String taskReference) {
        this.asyncTaksReferences.add(taskReference);
    }

    // CHECKSTYLE:OFF
    public enum WorkFlowStatus {

        WAITING(1), WORKING(2), CLOSED(3);

        private int status;

        private WorkFlowStatus(final int status) {
            this.status = status;
        };

        public int getStatusCode() {
            return status;
        }
    }
    // CHECKSTYLE:ON

}

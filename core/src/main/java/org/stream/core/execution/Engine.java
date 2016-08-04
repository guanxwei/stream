package org.stream.core.execution;

import org.stream.core.component.Node;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.ResourceType;

public interface Engine {

    /**
     * Use the graph with name graphName in the graphContext and execute on it. Clients should call the method {@link WorkFlow#close()} then {#
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName the graph name indicates which graph to use.
     * @param autoRecord implicit indicator to work flow engine if it should keep record foe each step.
     * @return new resource tank, which stores some resource.
     */
    public ResourceTank execute(final GraphContext graphContext,
            final String graphName,
            final boolean autoRecord,
            final ResourceType resourceType) throws WorkFlowExecutionExeception;

    /**
     * Use the graph with name graphName in the graphContext and execute on it. Clients will provide an
     * Resource instance so that the {@link Node} nodes in the graph can use it. The resource will be added to
     * the resouce tank of the current thread.
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName graphName the graph name indicates which graph to use.
     * @param primaryResource resource clients sent to the work flow engine, will be added to the new created resource tank.
     * @param autoRecord implicit indicator to work flow engine if it should keep record foe each step.
     * @return
     */
    public ResourceTank execute(final GraphContext graphContext,
            final String graphName,
            final Resource primaryResource,
            final boolean autoRecord,
            final ResourceType resourceType) throws WorkFlowExecutionExeception;

    /**
     * Use the graph with name graphName in the graphContext and execute on it. Clients will provide an
     * Resource instance so that the {@link Node} nodes in the graph can use it. The resource will be added to
     * the resouce tank of the current thread. Once the engine finish the work, it will automaticlly clean the desktroy
     * instance.
     * @param graphContext
     * @param graphName
     * @param resource
     * @param autoRecord
     * @param resourceType
     * @return
     * @throws WorkFlowExecutionExeception
     */
    public ResourceTank executeOnce(final GraphContext graphContext,
            final String graphName,
            final Resource primaryResource,
            final boolean autoRecord,
            final ResourceType resourceType) throws WorkFlowExecutionExeception;

    /**
     * Use the graph with name graphName in the graphContext and execute on it. Clients will not provide an
     * Resource instance. The resource will be added to
     * the resouce tank of the current thread. Once the engine finish the work, it will automaticlly clean the desktroy
     * instance.
     * @param graphContext
     * @param graphName
     * @param resource
     * @param autoRecord
     * @param resourceType
     * @return
     * @throws WorkFlowExecutionExeception
     */
    public ResourceTank executeOnce(final GraphContext graphContext,
            final String graphName,
            final boolean autoRecord,
            final ResourceType resourceType) throws WorkFlowExecutionExeception;

    /**
     * Clean up the engine, it needs lubrication! Clients should be very careful to invoke this method, once it is invoked, workflow information is cleaned up.
     * If the method is invoked in improper time unexpected fatal error will happen!. We provide an elegant way to substitude this method see {{@link #waitAndReboot()}.
     */
    public void reboot();

    /**
     * Wait for the working nodes to finish their work, then clean up the workflow.
     */
    public void waitAndReboot();
}

package org.stream.core.execution;

import org.stream.core.component.Node;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.ResourceType;

public interface Engine {

    /**
     * Use the graph with name graphName in the graphContext and execute on it. The workflow will not be cleaned up automatically after execution.
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName the name of graph which will be executed on.
     * @param autoRecord implicit indicator to work flow engine telling the engine if it need to record events automatically.
     * @param resourceType ignore it or just choose one from {@link ResourceType}.
     * @return new resource tank, which stores some resource.
     */
    public ResourceTank execute(final GraphContext graphContext,
            final String graphName,
            final boolean autoRecord,
            final ResourceType resourceType);

    /**
     * Use the graph with name graphName in the graphContext and execute on it. Clients should provide an
     * Resource instance so that the {@link Node} nodes in the graph can use it. The resource will be added to
     * the resouce tank of the current thread. The workflow will not be cleaned up automatically.
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName the name of graph which will be executed on.
     * @param primaryResource resource clients sent to the work flow engine, will be added to the new created resource tank.
     * @param autoRecord implicit indicator to work flow engine telling the engine if it need to record events automatically.
     * @param resourceType ignore it or just choose one from {@link ResourceType}.
     * @return new resource tank, which stores some resource.
     */
    public ResourceTank execute(final GraphContext graphContext,
            final String graphName,
            final Resource primaryResource,
            final boolean autoRecord,
            final ResourceType resourceType);

    /**
     * Use the graph with name graphName in the graphContext and execute on it. Clients should provide an
     * Resource instance so that the {@link Node} nodes in the graph can use it. The resource will be added to
     * the resouce tank of the current thread. Once the engine finish the work, it will automaticlly clean the desktroy
     * instance.
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName the name of graph which will be executed on.
     * @param primaryResource resource clients sent to the work flow engine, will be added to the new created resource tank.
     * @param autoRecord implicit indicator to work flow engine telling the engine if it need to record events automatically.
     * @param resourceType ignore it or just choose one from {@link ResourceType}.
     * @return
     */
    public ResourceTank executeOnce(final GraphContext graphContext,
            final String graphName,
            final Resource primaryResource,
            final boolean autoRecord,
            final ResourceType resourceType);

    /**
     * Use the graph with name graphName in the graphContext and execute on it. Clients will not provide an
     * Resource instance. The resource will be added to
     * the resouce tank of the current thread. Once the engine finish the work, it will automaticlly clean the desktroy
     * instance.
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName the name of graph which will be executed on.
     * @param autoRecord implicit indicator to work flow engine telling the engine if it need to record events automatically.
     * @param resourceType ignore it or just choose one from {@link ResourceType}.
     * @return new resource tank, which stores some resource.
     */
    public ResourceTank executeOnce(final GraphContext graphContext,
            final String graphName,
            final boolean autoRecord,
            final ResourceType resourceType);

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

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

import org.stream.core.component.Node;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;

/**
 * Abstract of work-flow engine, define APIs that all the implementations should provide.
 * @author guanxiong wei
 *
 */
public interface Engine {

    /**
     * Use the graph with name <p>graphName</p> in the graphContext and execute on it.
     * The work-flow will not be cleaned up automatically after execution.
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName The graph's name.
     * @param autoRecord implicit indicator telling the engine if it need to record events automatically.
     * @return New resource tank, which stores resources created during work-flow execution.
     */
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final boolean autoRecord);

    /**
     * Use the graph with name <p>graphName</p> in the graphContext and execute on it. Clients should provide an
     * Resource instance so that the {@link Node} nodes in the graph can use it. The resource will be added to
     * the resource tank of the current thread. The work-flow will not be cleaned up automatically.
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName The graph's name.
     * @param primaryResource resource clients sent to the work flow engine, will be added to the new created resource tank.
     * @param autoRecord implicit indicator telling the engine if it need to record events automatically.
     * @return New resource tank, which stores resources created during work-flow execution.
     */
    public ResourceTank execute(final GraphContext graphContext, final String graphName,
            final Resource primaryResource, final boolean autoRecord);

    /**
     * Use the graph with name <p>graphName</p> in the graphContext and execute on it. Clients should provide an
     * Resource instance so that the {@link Node} nodes in the graph can use it. The resource will be added to
     * the resource tank of the current thread. Once the engine finish the work, it will automatically clean the work-flow
     * instance.
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName the name of graph which will be executed on.
     * @param primaryResource resource clients sent to the work flow engine, will be added to the new created resource tank.
     * @param autoRecord implicit indicator telling the engine if it need to record events automatically.
     * @return New resource tank, which stores resources created during work-flow execution.
     */
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName,
            final Resource primaryResource, final boolean autoRecord);

    /**
     * Use the graph with name graphName in the graphContext and execute on it. Clients will not provide an
     * Resource instance. The resource will be added to
     * the resource tank of the current thread. Once the engine finish the work, it will automatically clean the work-flow
     * instance.
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName the name of graph which will be executed on.
     * @param autoRecord implicit indicator telling the engine if it need to record events automatically.
     * @return new resource tank, which stores some resource.
     */
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName, final boolean autoRecord);

    /**
     * Execute the graph from the specific start node.
     * @see #execute(GraphContext, String, boolean)
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName The graph's name.
     * @param startNode Start node instead of the graph's default start node.
     * @param autoRecord implicit indicator telling the engine if it need to record events automatically.
     * @return New resource tank, which stores resources created during work-flow execution.
     */
    public ResourceTank executeFrom(final GraphContext graphContext, final String graphName,
            final String startNode, final boolean autoRecord);

    /**
     * Execute the graph from the specific start node.
     * @see #execute(GraphContext, String, Resource, boolean)
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName The graph's name.
     * @param primaryResource resource clients sent to the work flow engine, will be added to the new created resource tank.
     * @param startNode Start node instead of the graph's default start node.
     * @param autoRecord implicit indicator telling the engine if it need to record events automatically.
     * @return New resource tank, which stores resources created during work-flow execution.
     */
    public ResourceTank executeFrom(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final String startNode, final boolean autoRecord);

    /**
     * Execute the graph from the specific start node.
     * @see #executeOnce(GraphContext, String, Resource, boolean)
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName the name of graph which will be executed on.
     * @param primaryResource resource clients sent to the work flow engine, will be added to the new created resource tank.
     * @param startNode Start node instead of the graph's default start node.
     * @param autoRecord implicit indicator telling the engine if it need to record events automatically.
     * @return New resource tank, which stores resources created during work-flow execution.
     */
    public ResourceTank executeOnceFrom(final GraphContext graphContext, final String graphName,
            final Resource primaryResource, final String startNode, final boolean autoRecord);

    /**
     * Execute the graph from the specific start node.
     * @see #executeOnce(GraphContext, String, boolean)
     * @param graphContext the graph Context from which the graph will be extracted.
     * @param graphName the name of graph which will be executed on.
     * @param autoRecord implicit indicator telling the engine if it need to record events automatically.
     * @param startNode Start node instead of the graph's default start node.
     * @return new resource tank, which stores some resource.
     */
    public ResourceTank executeOnceFrom(final GraphContext graphContext, final String graphName,
            final String startNode, final boolean autoRecord);

    /**
     * Stop the engine, it needs lubrication! Clients should be very careful to invoke this method,
     * once it is invoked, work-flow information will be cleaned up.
     * If the method is invoked in improper time, unexpected fatal error will happen!
     * We provide an elegant way to substitute this method, please see {{@link #waitAndReboot()}.
     *
     * @throws InterruptedException InterruptedException.
     */
    public void reboot() throws InterruptedException;

    /**
     * Wait for the working nodes to finish their work, then clean up the work-flow.
     */
    public void waitAndReboot();
}

package org.stream.core.execution;

import org.stream.core.component.Graph;

import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced graph context of {@link GraphContext} enable modifying the graph configuration in run-time.
 * @author weiguanxiong
 *
 */
@Slf4j
public class DynamicGraphContext extends GraphContext {

    /**
     * Add a graph to the graph context.
     * @param graph Graph instance to be added.
     */
    @Override
    public void addGraph(final Graph graph) {
        if (graphs.containsKey(graph.getGraphName())) {
            Graph originalGraph = graphs.get(graph.getGraphName());
            log.info("Graph [{}] is changed, original definition \n [{}] \n while updated definition is \n [{}]",
                    graph.getGraphName(), originalGraph.getOriginalDefinition(), graph.getOriginalDefinition());
        }
        graphs.put(graph.getGraphName(), graph);
    }
}

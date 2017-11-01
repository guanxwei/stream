package org.stream.core.component;

import java.util.List;

import org.stream.core.resource.ResourceType;

import lombok.Data;

/**
 * Encapsulation of work-flow execution procedure. Each graph should be defined in a stand-alone file
 * with suffix ".graph". Work-flow run time context will load these files and assemble them into {@linkplain Graph}
 * instance so the {@linkplain Engine} can use them to complete the designed work.
 *
 */
@Data
public class Graph {

    /**
     * The name of the graph. Every graph should have a unique name or it may cause unexpected errors.
     */
    private String graphName;

    /**
     * The nodes that this graph contains, including default error handling node & start node & .etc.
     */
    private List<Node> nodes;

    /**
     * The resourceType of the graph nodes execute on, see {@link ResourceType}.
     */
    private ResourceType resourceType;

    /**
     * The entry point of the graph, a work flow execution engine will choose this node to execute first.
     */
    private Node startNode;

    /**
     * The default error handling node of the graph. It will be invoked in scenarios when a node throws exception or
     * a node returns {@link ActivityResult.FAIL} while it not specify its own error handling node.
     * When a node returns fail result, before the work-flow engine exits, the error handling node should help do some clean work.
     */
    private Node defaultErrorNode;

}

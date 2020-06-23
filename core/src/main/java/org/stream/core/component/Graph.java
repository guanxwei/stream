package org.stream.core.component;

import java.util.List;

import org.stream.core.execution.Engine;
import org.stream.core.helper.GraphConfiguration;

import lombok.Data;

/**
 * Encapsulation of work-flow execution procedure definition. Each graph represents single one procedure, which should be defined in a stand-alone file
 * with suffix ".graph". Work-flow run time context will load these files and assemble them into {@linkplain Graph}
 * instances so the {@linkplain Engine} can use them to complete the designed work.
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
     * The entry point of the graph, the work flow execution engine will choose this node to execute first.
     */
    private Node startNode;

    /**
     * The default error handler node of the graph. It will be invoked in scenarios when a node throws exception or
     * a node returns {@link ActivityResult.FAIL} while it does not specify its own error handling node.
     * When a node returns fail result, before the work-flow engine exits, the error handling node should help do some clean work.
     *
     * Default error node should never throw any exception and can only return {@link ActivityResult#SUCCESS}, otherwise the programe
     * will run into dead circle causing crash.
     */
    private Node defaultErrorNode;

    /**
     * Original definition of the graph.
     */
    private String originalDefinition;

    /**
     * Please refer to {@link GraphConfiguration#getPrimaryResourceType()}
     */
    private String primaryResourceType;

    /**
     * Detail description of graph, mainly used to tell the users how this graph should be used
     * what the procedure will be, etc.
     */
    private String description;

    /**
     * Retrieve a node from the graph by node name.
     * @param nodeName Target node's configured name in graph file.
     * @return Target node.
     */
    public Node getNode(final String nodeName) {
        for (Node node : nodes) {
            if (nodeName.equalsIgnoreCase(node.getNodeName())) {
                return node;
            }
        }
        return null;
    }
}

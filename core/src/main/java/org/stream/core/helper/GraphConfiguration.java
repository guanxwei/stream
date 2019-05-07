package org.stream.core.helper;

import org.stream.core.component.ActivityResult;
import org.stream.core.execution.AutoScheduledEngine;

import lombok.Data;

/**
 * Graph configuration encapsulation, which is used to initiate a graph.
 * Graph configuration information is retrieved from a
 * graph definition file, which has a suffix ".graph"
 */
@Data
public class GraphConfiguration {

    /**
     * Graph name, after the graph is loaded, the application can use the graph by getting from the graph context using
     * the graph name specified here.
     */
    private String graphName;

    /**
     * The first node to be executed.
     */
    private String startNode;

    /**
     * The nodes defined in the graph file.
     */
    private NodeConfiguration[] nodes;

    /**
     * Default error handler node. If error node of the target node is not specified the workflow engine will
     * try to execute this node(if specified) when the target node returns {@link ActivityResult#FAIL} or throws
     * an exception.
     */
    private String defaultErrorNode;

    /**
     * {@link AutoScheduledEngine} used only attributes indicates the type of input primary resource's type, the underlying
     * Serializing framework will use it to serialize the resource when communicate with the remote actors.
     */
    private String primaryResourceType;
}

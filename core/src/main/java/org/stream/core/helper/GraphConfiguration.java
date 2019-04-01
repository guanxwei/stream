package org.stream.core.helper;

import org.stream.core.execution.AutoScheduledEngine;

import lombok.Data;

/**
 * Graph configuration encapsulation, which is used to initiate a graph.
 * Graph configuration information is retrieved from a
 * graph definition file, which has a suffix ".graph"
 */
@Data
public class GraphConfiguration {

    private String graphName;

    private String resourceType;

    private String startNode;

    private NodeConfiguration[] nodes;

    private String defaultErrorNode;

    /**
     * {@link AutoScheduledEngine} used only attributes indicates the type of input primary resource's type, the underlying
     * Serializing framework will use it to serialize the resource when communicate with the remote actors.
     */
    private String primaryResourceType;
}

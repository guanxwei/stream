package org.stream.core.helper;

import lombok.Data;

/**
 * Graph configuration encapsulation, which is used to initiate a graph. Graph configuration information is retrieved from a 
 * graph definition file, which has a suffix like ".graph"
 */
@Data
public class GraphConfiguration {

    private String graphName;

    private String resourceType;

    private String startNode;

    private NodeConfiguration[] nodes;

    private String defaultErrorNode;
}

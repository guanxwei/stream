package org.stream.core.helper;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Node configuration encapsulation, which is used to initiate a node in a graph.
 */
@Data
public class NodeConfiguration {

    private String nodeName;

    private String activityClass;

    private String successNode;

    private String failNode;

    private String suspendNode;

    private AsyncNodeConfiguration[] asyncDependencies;

    /**
     * Encapsulation of asynchronous Node configuration, which is used to initiate a asynchronous node.
     * @author hzweiguanxiong
     *
     */
    public static class AsyncNodeConfiguration {
        @Setter @Getter
        private String asyncNode;
    }
}

package org.stream.core.helper;

import java.util.List;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.AutoScheduledEngine;
import org.stream.extension.io.Tower;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Node configuration encapsulation, which is used to initiate a node in a graph.
 */
@Data
public class NodeConfiguration {

    /**
     * The node's name, precise of the purpose of the node.
     */
    private String nodeName;

    /**
     * Activity class, basically it should be a sub-class of {@link Activity}.
     *
     */
    private String activityClass;

    /**
     * The successor node when this node returns {@link ActivityResult#SUCCESS}.
     */
    private String successNode;

    /**
     * The successor node when this node returns {@link ActivityResult#FAIL}.
     * If the failNode is not specified, the default error node will be invode if it is
     * defined in the graph.
     */
    private String failNode;

    /**
     * The successor node when this node returns {@link ActivityResult#SUSPEND}.
     */
    private String suspendNode;

    /**
     * The successor node when this node returns {@link ActivityResult#UNKNOWN}.
     */
    private String checkNode;

    /**
     * The references to the tasks should be ran asynchronously.
     * No matter these tasks succeed or fail, the main node should be effected.
     */
    private AsyncNodeConfiguration[] asyncDependencies;

    /**
     * Time interval list used to introduce the {@link AutoScheduledEngine} when to
     * retry the node's action then the node returns {@link ActivityResult#SUSPEND}.
     */
    private List<Integer> intervals;

    /**
     * Actor provider, basically should be a sub-class of {@link Tower}.
     */
    private String actorClass;

    /**
     * Detail description of the purpose of the node.
     */
    private String description;

    /**
     * Encapsulation of asynchronous Node configuration, which is used to initiate a asynchronous node.
     * @author hzweiguanxiong
     *
     */
    public static class AsyncNodeConfiguration {
        @Setter @Getter
        private String asyncNode;

        @Setter @Getter
        private long timeout;
    }

    /**
     * Return the real provider class of this node.
     * Will use activity class if it is present, otherwise use actor class instead.
     * @return Provider class name.
     */
    public String getProviderClass() {
        if (activityClass != null) {
            return activityClass;
        }

        return actorClass;
    }
}

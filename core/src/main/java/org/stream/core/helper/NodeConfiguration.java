package org.stream.core.helper;

import java.util.List;

import org.stream.core.component.Activity;
import org.stream.extension.io.Tower;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Node configuration encapsulation, which is used to initiate a node in a graph.
 */
@Data
public class NodeConfiguration {

    private String nodeName;

    /**
     * Activity class, basically it should be a sub-class of {@link Activity}.
     *
     */
    private String activityClass;

    private String successNode;

    private String failNode;

    private String suspendNode;

    private String checkNode;

    private AsyncNodeConfiguration[] asyncDependencies;

    private List<Integer> intervals;

    /**
     * Actor provider, basically should be a sub-class of {@link Tower}.
     */
    private String actorClass;

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

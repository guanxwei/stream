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

package org.stream.core.helper;

import java.util.List;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.Condition;
import org.stream.core.component.SubFlow;
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
     * Activity class, basically it should be a subclass of {@link Activity}.
     *
     */
    private String activityClass;

    /**
     * The successor node when this node returns {@link ActivityResult#SUCCESS}.
     */
    private String successNode;

    /**
     * The successor node when this node returns {@link ActivityResult#FAIL}.
     * If the failed bode is not specified, the default error node will be invoked if it is
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
     * The references to the tasks should be run asynchronously.
     * These async tasks will be canceled if they are not finished before the workflow shutdown.
     */
    private AsyncNodeConfiguration[] asyncDependencies;

    /**
     * The references to the tasks should be run asynchronously and will not be canceled when the workflow returns.
     */
    private DaemonNodeConfiguration[] daemons;

    /**
     * Time interval list used to introduce the {@link AutoScheduledEngine} when to
     * retry the node's action then the node returns {@link ActivityResult#SUSPEND}.
     */
    private List<Integer> intervals;

    /**
     * Actor provider, basically should be a subclass of {@link Tower}.
     */
    private String actorClass;

    /**
     * Detail description of the purpose of the node.
     */
    private String description;

    /**
     * Condition configuration.
     */
    private List<Condition> conditions;

    /**
     * Target child graphs that probably be executed after this node.
     */
    private List<SubFlow> subflows;

    /**
     * The flag indicated it the node is degradable,
     * once the engine found that this node is not available and the flag is set as true,
     * the engine may skip execute this node until it's available again.
     */
    private boolean degradable;

    /**
     * Sentinel configuration.
     */
    private SentinelConfiguration sentinelConfiguration;

    /**
     * Encapsulation of asynchronous Node configuration, which is used to initiate an asynchronous node.
     * These async activities will be shutdown once the workflow is finished.
     * If you want to keep the async works
     * alive after the workflow finished, use the long daemon node configuration.
     * @author guanxiong wei
     *
     */
    @Setter
    @Getter
    public static class AsyncNodeConfiguration {
        private String asyncNode;

        private long timeout;
    }

    /**
     * Encapsulation of asynchronous Node configuration, not like the async nodes, the activities will not be canceled until they are completed.
     */
    @Setter
    @Getter
    public static class DaemonNodeConfiguration {
        private String daemonNode;
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

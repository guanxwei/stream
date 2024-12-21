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

package org.stream.extension.intercept;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.Node;
import org.stream.core.execution.AutoScheduledEngine;

/**
 * Interceptors that will intercept the workflow execution procedure users can use
 * this abstract to help control the {@link Activity} behaviors. For example, if
 * some of the service providers crashed when using {@link AutoScheduledEngine}, administrator
 * may take action like fast-fail to prevent the system blocked. Then a fast-fail detected interceptor
 * can be plugged in the workflow context, once the detector finds that the downstream service crashes, it
 * can throw an exception instead of invoking the service in case all the threads blocked by the crashed service.
 * <p>
 * Interceptors will be invoked before the action (if any are configured) in the current node's activity.
 * @author weiguanxiong
 *
 */
public interface Interceptor {

    /**
     * Action to be invoked before the node's activity.
     * @param currentNode Current node.
     */
    void before(final Node currentNode);

    /**
     * Action to be invoked after the node's activity.
     * @param currentNode Current node.
     * @param activityResult Activity result.
     */
    void after(final Node currentNode, final ActivityResult activityResult);

    /**
     * Action to be triggered when an unexpected error arose from the node.
     * @param currentNode Current execution node.
     * @param t Throwable from the node.
     */
    void onError(final Node currentNode, final Throwable t);

    /**
     * Return the target graph name, so that we can filter non-used interceptors for
     * every node execution.
     * @return Target graph name.
     */
    String targetGraph();
}

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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Node;

import lombok.NonNull;

/**
 * Interceptors facility.
 */
public final class Interceptors {

    private static final Map<String, List<Interceptor>> REGISTERED_INTERCEPTORS = new HashMap<>();

    private Interceptors() { }

    /**
     * Merge more interceptors.
     * @param elements New added interceptors.
     */
    public static void merge(@NonNull  Map<String, List<Interceptor>> elements) {
        List<Interceptor> merged = new LinkedList<>();
        for (Entry<String, List<Interceptor>> entry : elements.entrySet()) {
            if (REGISTERED_INTERCEPTORS.containsKey(entry.getKey())) {
                merged.addAll(REGISTERED_INTERCEPTORS.get(entry.getKey()));
            }
            merged.addAll(entry.getValue());
            REGISTERED_INTERCEPTORS.put(entry.getKey(), merged);
        }
    }

    /**
     * Invoke interceptors before we're executing the target node.
     * @param node Node to be executed.
     */
    public static void before(final Node node) {
        onTemplate(null, node, 1);
    }

    /**
     * Action to be invoked after the node's activity.
     * @param node Node just been executed.
     * @param activityResult Activity result.
     */
    public static void after(final Node node, final ActivityResult activityResult) {
        onTemplate(activityResult, node, 2);
    }

    /**
     * Action to be triggered when an unexpected error arose from the node.
     * @param node Current node.
     * @param t Throwable from the node.
     */
    public static void onError(final Node node, final Throwable t) {
        onTemplate(t, node, 3);
    }

    private static void onTemplate(final Object input, final Node node, final int type) {
        var graph = node.getGraph();
        var graphName = graph.getGraphName();
        
        var candidates = REGISTERED_INTERCEPTORS.get(graphName);
        if (candidates == null) return; // No interceptors configured.
        for (Interceptor interceptor : candidates) {
            switch (type) {
                case 1:
                    interceptor.before(node);
                    break;
                case 2:
                    interceptor.after(node, (ActivityResult) input);
                    break;
                case 3:
                    interceptor.onError(node, (Throwable) input);
                    break;
                default:
                    break;
            }
        }
    }
}
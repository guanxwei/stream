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

package org.stream.core.execution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityRepository;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;

import lombok.Setter;

/**
 * Encapsulation of graph context, generally one application holds an graph context instance.
 * If there are mutil applications running
 * in the same JVM, then there should be multi {@link GraphContext} instances.
  */
public class GraphContext {

    @Setter
    private ActivityRepository activityRepository;

    protected Map<String, Graph> graphs = new ConcurrentHashMap<>();

    /**
     * Check if an {@link Activity} instance exist in the graph context.
     * Generally, each {@link Activity} instance should be related to an {@link Node} instance, meanwhile that {@link Node}
     * instance should also be related to an {@link Graph} instance. Graphs should not share Nodes.
     * @param activityName The activity's name, it will default be the Activity's class name.
     * @return Checking result.
     */
    public boolean isActivityRegistered(final String activityName) {
        return activityRepository.isActivityRegistered(activityName);
    }

    /**
     * Get an exist activity from the activity repository.
     * @param activityName The activity's name, it will default be the Activity's class name.
     * @return Activity instance having name {@literal activityName} {@linkplain}
     */
    public Activity getActivity(final String activityName) {
        return activityRepository.getActivity(activityName);
    }

    /**
     * Add a graph to the graph context.
     * @param graph Graph instance to be added.
     */
    public void addGraph(final Graph graph) {
        graphs.putIfAbsent(graph.getGraphName(), graph);
    }

    /**
     * Get a graph instance from the graph context by name.
     * @param graphName The graph instance's name.
     * @return {@linkplain Graph} instance.
     */
    public Graph getGraph(final String graphName) {
        return graphs.get(graphName);
    }

    /**
     * Return how many graphs have been registered in the context.
     * @return The quantity of graphs registered in the context.
     */
    public int getGraphRegistered() {
        return graphs.size();
    }

    /**
     * Register a activity in the context.
     * {@link GraphContext} will delegate the work to the underline {@link ActivityRepository} hosted in this context.
     * @param activity Activity to be registered.
     */
    public void registerActivity(final Activity activity) {
        activityRepository.register(activity);
    }
}

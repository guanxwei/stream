package org.stream.core.execution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityRepository;
import org.stream.core.component.Graph;

import lombok.Setter;

/**
 * Encapsulation of graph context, generally one application holds an graph context instance. If there are mutil applications running
 * in the same JVM, then there should be multi {@link GraphContext} instances.
  */
public class GraphContext {

    @Setter
    private ActivityRepository activityRepository;

    private Map<String, Graph> graphs = new ConcurrentHashMap<String, Graph>();

    /**
     * Check if an {@link Activity} instance exist in the graph context. Generally, an {@link Activity} instance should be related to an {@link Node} instance, meanwhile that {@link Node}
     * instance should be related to an {@link Graph} instance. Graphs should not share Nodes. 
     * @param activityName an activity's name, defaultly it is the Activity's class name.
     * @return
     */
    public boolean isActivityRegistered(final String activityName) {
        return activityRepository.isActivityRegistered(activityName);
    }

    /**
     * Get an exist activity from the activity repository.
     * @param activityName
     * @return
     */
    public Activity getActivity(final String activityName) {
        return activityRepository.getActivity(activityName);
    }

    /**
     * Add a graph to the graph context.
     * @param graph
     */
    public void addGraph(final Graph graph) {
        graphs.putIfAbsent(graph.getGraphName(), graph);
    }

    /**
     * Get a graph instance from the graph context.
     * @param graphName
     * @return 
     */
    public Graph getGraph(final String graphName) {
        return graphs.get(graphName);
    }

    /**
     * Return how many graphs has been registered in the context.
     * @return
     */
    public int getGraphRegistered() {
        return graphs.size();
    }

    /**
     * Re
     * @param activity
     */
    public void registerActivity(final Activity activity) {
        activityRepository.register(activity);
    }
}

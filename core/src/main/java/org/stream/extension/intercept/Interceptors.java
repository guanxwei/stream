package org.stream.extension.intercept;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;

import lombok.NonNull;

/**
 * Interceptors facility.
 */
public final class Interceptors {

    private static final Map<String, List<Interceptor>> REGISTERD_INTERCEPTORS = new HashMap<>();

    private Interceptors() { }

    /**
     * Merge more interceptors.
     * @param elements New added interceptors.
     */
    public static void merge(@NonNull  Map<String, List<Interceptor>> elements) {
        List<Interceptor> merged = new LinkedList<>();
        for (Entry<String, List<Interceptor>> entry : elements.entrySet()) {
            if (REGISTERD_INTERCEPTORS.containsKey(entry.getKey())) {
                merged.addAll(REGISTERD_INTERCEPTORS.get(entry.getKey()));
            }
            merged.addAll(entry.getValue());
            REGISTERD_INTERCEPTORS.put(entry.getKey(), merged);
        }
    }

    /**
     * Invoke interceptors before we executing the target node.
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
     * Action to be triggered when unexpected error arosen from the node.
     * @param node Current node.
     * @param t Throwable from the node.
     */
    public static void onError(final Node node, final Throwable t) {
        onTemplate(t, node, 3);
    }

    private static void onTemplate(final Object input, final Node node, final int type) {
        Graph graph = node.getGraph();
        String graphName = graph.getGraphName();
        List<Interceptor> candidates = REGISTERD_INTERCEPTORS.get(graphName);
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
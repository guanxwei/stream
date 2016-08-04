package org.stream.core.resource;

import java.util.concurrent.ConcurrentHashMap;

public class ResourceTank {

    /**
     * Graph instance resources store, once the workflow begin to execute a graph, the engine will initiate a thread local
     * variable to store {@linkplain Resource} for the execution request.
     * If an internal Node a graph will invoke another graph, then both these two graph instances will share one resource tank,
     * which is determined by the entry point of the execution request.
     */
    private ConcurrentHashMap<String,Resource> resources = new ConcurrentHashMap<String, Resource>();

    /**
     * Add a new resource object into the resource tank.
     * @param resource
     * @return
     */
    public void addResource(final Resource resource) {
        resources.put(resource.getResourceReference(), resource);
    }

    /**
     * Extract an {@link Resource} from the resource tank by its resource reference.
     * @param resourceReference
     * @return
     */
    public Resource resolve(final String resourceReference) {
        return resources.get(resourceReference);
    }

}

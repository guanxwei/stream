package org.stream.core.resource;

import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;

/**
 * A tank to store {@link Resource} instances in memory.
 * @author guanxiong wei
 *
 */
public class ResourceTank {

    /**
     * Graph instance hold resources storage, once the work-flow begin to execute on a graph, the engine will initiate a thread local
     * variable to store {@linkplain Resource}s for the execution request.
     * If an internal Node of the graph will invoke another graph, then both these two graph instances will share that one resource tank,
     * determined at the entry point of the execution procedure.
     */
    @Getter
    private ConcurrentHashMap<String, Resource> resources = new ConcurrentHashMap<>();

    /**
     * Add a new resource object into the resource tank.
     * @param resource Resource to added to the tank.
     */
    public void addResource(final Resource resource) {
        if (resource.getResourceURL() != null) {
            resources.put(resource.getResourceURL().getPath(), resource);
        } else {
            resources.put(resource.getResourceReference(), resource);
        }
    }

    /**
     * Extract an {@link Resource} from the resource tank by its resource reference.
     * @param resourceReference Resource reference.
     * @return Resource instance if existed.
     */
    public Resource resolve(final String resourceReference) {
        return resources.get(resourceReference);
    }

    /**
     * Remove a resource.
     * @param resourceReference Resource reference.
     */
    public void remove(final String resourceReference) {
        resources.remove(resourceReference);
    }
}

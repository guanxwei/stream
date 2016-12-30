package org.stream.core.resource;

import java.util.HashMap;
import java.util.Map;

import org.stream.core.exception.ResourceReadingExecption;

import lombok.Getter;
import lombok.Setter;

/**
 * Resource catalog used to store resources and fetch resources, basicly it delegate the action to the underlying {@link ResourceReader}.
 * customers should implements {@link ResourceReader} to realize their own data storing and fetching logic.
 * Customers can choose to store and fetch the resources from a RDB or NoSQL or whatever, they should realize the way how
 * to get the real value of the {@link Resource} then store it to their data repository, also they should realize the way
 * how to retreive the real value by {@link ResourceURL} then decorate it into an standard instance of {@link Resource} so that
 * other components in the framework can use it freely.
 */
public class ResourceCatalog {

    private Cache cache;

    @Getter @Setter
    private Map<String, ResourceReader> readers = new HashMap<>();

    /**
     * Register a resource reader instance.
     * @param reader The reader instance needed to registered.
     */
    public void registerReader(ResourceReader reader) {
        this.readers.put(reader.resolve().getValue(), reader);
    }

    private ResourceReader resolve(ResourceURL resourceURL) {
        return readers.get(resourceURL.getResourceAuthority().getValue());
    }

    /**
     * Read a resource from the resource catalog by its resourceURL.
     * @param resourceURL The resourceURL.
     * @return The retrieved resource from the resource storage.
     * @throws ResourceReadingExecption 
     */
    public Resource readResource(ResourceURL resourceURL) throws ResourceReadingExecption {
        if (resourceURL == null || resourceURL.getResourceAuthority() == null) {
            throw new ResourceReadingExecption("ResourceURL should not be null!");
        }
        ResourceReader reader = resolve(resourceURL);
        if (reader == null) {
            throw new ResourceReadingExecption(String.format("There is no reader registered for this resourceURL represented resource authority [%s]", resourceURL.getResourceAuthority().getValue()));
        }
        return reader.read(resourceURL);
    }

    /**
     * Retrieve the resource the memory cache speeding up performance.
     * @param resourceURL
     * @return
     */
    public Resource readResourceFromCache(ResourceURL resourceURL) {
        return cache.get(resourceURL);
    }

    /**
     * Test if the resource is expired.
     * @param resource
     * @return
     */
    public boolean isResourceExpired(Resource resource) {
        return cache.isResourceExpired(resource);
    }

    /**
     * Mark the resource as expired so that customers give up to use it.
     * @param resource
     */
    public void setResourceExpired(Resource resource) {
        cache.setResourceExpired(resource);
    }

}

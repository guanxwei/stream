package org.stream.core.resource;

import java.util.HashMap;
import java.util.Map;

import org.stream.core.exception.ResourceReadingExecption;

import lombok.Getter;
import lombok.Setter;

/**
 * Resource catalog used to store resources and fetch resources, basically it will delegate the action to
 * the underlying {@link ResourceReader}s.
 * Customers should implements {@link ResourceReader} to realize their own data storing and fetching logic.
 *
 * As for the underlying "storage later", Stream work-flow does not specify any limitations.
 * Customers can choose to store and fetch the resources from a RDB or NoSQL or whatever, they should realize the way how
 * to get the real value of the {@link Resource} then store it to their data repository, also they should realize the way
 * how to retrieve the real value by {@link ResourceURL} then decorate it into an standard instance of {@link Resource} so that
 * other components in the framework can use it freely.
 */
public class ResourceCatalog {

    @Setter
    @Getter
    private Cache cache;

    @Getter @Setter
    private Map<String, ResourceReader> readers = new HashMap<>();

    /**
     * Register a resource reader instance.
     * @param reader The reader instance needed to registered.
     */
    public void registerReader(final ResourceReader reader) {
        this.readers.put(reader.resolve().getValue(), reader);
    }

    private ResourceReader resolve(final ResourceURL resourceURL) {
        return readers.get(resourceURL.getResourceAuthority().getValue());
    }

    /**
     * Read a resource from the resource catalog by its resourceURL.
     * @param resourceURL The resourceURL.
     * @return The retrieved resource from the resource storage.
     * @throws ResourceReadingExecption ResourceReadingExecption.
     */
    public Resource readResource(final ResourceURL resourceURL) throws ResourceReadingExecption {
        if (resourceURL == null || resourceURL.getResourceAuthority() == null) {
            throw new ResourceReadingExecption("ResourceURL should not be null!");
        }
        ResourceReader reader = resolve(resourceURL);
        if (reader == null) {
            throw new ResourceReadingExecption(String.format("There is no reader registered for this resourceURL represented resource authority [%s]",
                    resourceURL.getResourceAuthority().getValue()));
        }
        Resource resource = reader.read(resourceURL);
        if (cache != null) {
            cache.put(resourceURL, resource);
        }
        return resource;
    }

    /**
     * Retrieve the resource from the memory cache speeding up performance.
     * @param resourceURL Resource url.
     * @return Resource.
     * @throws ResourceReadingExecption ResourceReadingExecption.
     */
    public Resource readResourceFromCache(final ResourceURL resourceURL) throws ResourceReadingExecption {
        if (cache == null) {
            throw new ResourceReadingExecption("Cache is not configured!");
        }
        return cache.get(resourceURL);
    }

    /**
     * Test if the resource is expired.
     * @param resource Resource to be tested.
     * @return Checking result.
     * @throws ResourceReadingExecption ResourceReadingExecption.
     */
    public boolean isResourceExpired(final Resource resource) throws ResourceReadingExecption {
        if (cache == null) {
            throw new ResourceReadingExecption("Cache is not configured!");
        }
        return cache.isResourceExpired(resource);
    }

    /**
     * Mark the resource as expired.
     * @param resource Resource be to set as expired.
     * @throws ResourceReadingExecption ResourceReadingExecption.
     */
    public void setResourceExpired(final Resource resource) throws ResourceReadingExecption {
        if (cache == null) {
            throw new ResourceReadingExecption("Cache is not configured!");
        }
        cache.setResourceExpired(resource);
    }

}

package org.stream.core.resource;

/**
 * Abstraction of cache used to cache resources in the framework. Customers can implements it in their way to fulfill special requirement.
 * Here we only exposes some key APIs that customers need to implement, the detail how they arrange the cached object like reading cache object
 * making cached object expired .etc depends on their design.
 */
public interface Cache {

    /**
     * Retrieve resource from the cache via its URL.
     * @param resourceURL resource URL reference to the resource.
     * @return the target resource or null if the resource does not exist in the cache pool.
     */
    Resource get(final ResourceURL resourceURL);

    /**
     * Put a resource to the cache pool, so that it can be retrieved faster in the following operations.
     * @param resourceURL the url referred to the resource.
     * @param resource resource need to be put to the cache pool.
     */
    void put(final ResourceURL resourceURL, final Resource resource);

    /**
     * Check if the resource has been marked as expired. Concrete implementation can determine their own mechanism to realize this character,
     * and they should obey to their usage principle when they plug their implementation into real software system.
     * @param resource Target resource.
     * @return Checking result.
     */
    boolean isResourceExpired(final Resource resource);

    /**
     * Mark the resource as expired. Also please refer to {{@link #isResourceExpired(Resource)}, these two methods should keep in coordination
     * to work properly. Framework does not provide any guarantee what exact work will be done,
     * it all depends on the concrete implementation.
     * @param resource resource need to be set as expired.
     */
    void setResourceExpired(final Resource resource);

}

package org.stream.core.resource;

/**
 * Abstraction of cache used to cache resources in the framework. Customers can implements it in their way to fulfill specitial requirement.
 * Here we only exposes some key interface that customers need to implement, the detail how they arrange the cached object like reading cache object
 * making cached object expired .etc depends on their design.
 */
public interface Cache {

    /**
     * Retreive resource from the cache via its url.
     * @param resourceURL resource url reference to the resource.
     * @return the target resource or null if the resource does not exist in the cache pool.
     */
    Resource get(ResourceURL resourceURL);

    /**
     * Put a resource to the cache pool, so that it can be retreived faster in the following operations.
     * @param resource resource need to be put to the cache pool.
     */
    void put(Resource resource);

    /**
     * Check if the resource has been marked as expirded. Concret implementation can determine their own mechanism to realize this charactor,
     * and they should obey to their usage principle when they plug their implementation into real software system.
     * @param resource
     * @return
     */
    boolean isResourceExpired(Resource resource);

    /**
     * Mark the resource as expirded. Also please refer to {{@link #isResourceExpired(Resource)}, these two methods should keep in coordination
     * to it work at proper status. Framework does not provide any guarantee that what the real situation is going, it all depends on the concret implementation.
     * @param resource
     */
    void setResourceExpired(Resource resource);

}

package org.stream.core.resource.sample;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.stream.core.resource.Cache;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceURL;

public class MemoryCache implements Cache {

    private Map<String, SoftReference<Resource>> cached_resources = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource get(ResourceURL resourceURL) {
        SoftReference<Resource> reference = cached_resources.get(resourceURL.getPath());
        if (reference != null) {
            return reference.get();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(ResourceURL resourceURL, Resource resource) {
        SoftReference<Resource> reference = new SoftReference<Resource>(resource);
        cached_resources.put(resourceURL.getPath(), reference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResourceExpired(Resource resource) {
        /**
         * We don't provide mechanism to help check if the cached object is expired or not, we just return true if you ask us.
         */
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceExpired(Resource resource) {
        if (cached_resources.containsKey(resource.getResourceURL().getPath())) {
            cached_resources.remove(resource.getResourceURL().getPath());
        }
    }

}

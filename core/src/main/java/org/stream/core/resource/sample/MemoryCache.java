package org.stream.core.resource.sample;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.stream.core.resource.Cache;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceURL;

/**
 * Sample implementation of {@link Cache}.
 * Only used for purpose of illustration, please do not use in online environments.
 * @author guanxiong wei
 *
 */
public class MemoryCache implements Cache {

    private static final Map<String, SoftReference<Resource>> CACHED_RESOURCES = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource get(final ResourceURL resourceURL) {
        SoftReference<Resource> reference = CACHED_RESOURCES.get(resourceURL.getPath());
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
    public void put(final ResourceURL resourceURL, final Resource resource) {
        SoftReference<Resource> reference = new SoftReference<Resource>(resource);
        CACHED_RESOURCES.put(resourceURL.getPath(), reference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResourceExpired(final Resource resource) {
        /**
         * We don't provide mechanism to help check if the cached object is expired or not, we just return true if you ask us.
         */
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceExpired(final Resource resource) {
        if (CACHED_RESOURCES.containsKey(resource.getResourceURL().getPath())) {
            CACHED_RESOURCES.remove(resource.getResourceURL().getPath());
        }
    }

}

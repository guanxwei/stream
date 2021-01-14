package org.stream.core.resource.sample;

import java.util.concurrent.TimeUnit;

import org.stream.core.resource.Cache;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceURL;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Sample implementation of {@link Cache}.
 * Only used for purpose of illustration, please do not use in online environments.
 * @author guanxiong wei
 *
 */
public class MemoryCache implements Cache {

    private static final LoadingCache<String, Resource> CACHE = CacheBuilder.newBuilder()
                .concurrencyLevel(200)
                .expireAfterAccess(3000, TimeUnit.MILLISECONDS)
                .build(new CacheLoader<String, Resource>() {
                        public Resource load(final String reference) throws Exception {
                            return null;
                        }
                });

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource get(final ResourceURL resourceURL) {
        return CACHE.getUnchecked(resourceURL.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final ResourceURL resourceURL, final Resource resource) {
        CACHE.put(resourceURL.getPath(), resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResourceExpired(final Resource resource) {
        return CACHE.getUnchecked(resource.getResourceURL().getPath()) == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceExpired(final Resource resource) {
        CACHE.invalidate(resource.getResourceURL().getPath());
    }

}

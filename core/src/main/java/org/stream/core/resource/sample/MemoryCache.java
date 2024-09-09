/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stream.core.resource.sample;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.stream.core.resource.Cache;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceURL;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.extern.slf4j.Slf4j;

/**
 * Sample implementation of {@link Cache}.
 * Only used for purpose of illustration, please do not use in online environments.
 * @author guanxiong wei
 *
 */
@Slf4j
public class MemoryCache implements Cache {

    private static final LoadingCache<String, Resource> CACHE = CacheBuilder.newBuilder()
                .concurrencyLevel(200)
                .expireAfterAccess(60 * 1000, TimeUnit.MILLISECONDS)
                .maximumSize(1000)
                .build(new CacheLoader<String, Resource>() {
                        public Resource load(final @Nonnull String reference) throws Exception {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final ResourceURL resourceURL, final Resource resource, final int ttl) {
        log.warn("TTL parameter does not work, cache not supported, the cache will be removed automatically"
                + " after 1 minute since the last time it is accessed");
        CACHE.put(resourceURL.getPath(), resource);
    }

}

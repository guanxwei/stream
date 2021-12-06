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

import org.stream.core.resource.Cache;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceURL;
import org.stream.extension.clients.RedisClient;

/**
 * A cache using redis as remote data storage.
 * @author guanxiongwei
 *
 */
public class RedisCache implements Cache {

    @javax.annotation.Resource
    private RedisClient redisClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource get(final ResourceURL resourceURL) {
        String content = redisClient.get(resourceURL.getPath());
        return Resource.parse(content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final ResourceURL resourceURL, final Resource resource) {
        redisClient.set(resourceURL.getPath(), resource.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResourceExpired(final Resource resource) {

        assert resource.getResourceURL() != null;

        return redisClient.get(resource.getResourceURL().getPath()) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceExpired(final Resource resource) {
        redisClient.del(resource.getResourceURL().getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(ResourceURL resourceURL, Resource resource, int ttl) {
        redisClient.setWithExpireTime(resourceURL.getPath(), resource.toString(), ttl);
    }
}

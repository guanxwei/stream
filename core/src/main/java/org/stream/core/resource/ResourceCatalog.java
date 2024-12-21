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

package org.stream.core.resource;

import java.util.HashMap;
import java.util.Map;

import org.stream.core.exception.ResourceReadingException;

import lombok.Getter;
import lombok.Setter;

/**
 * Resource catalog used to store resources and fetch resources, basically it will delegate the action to
 * the underlying {@link ResourceReader}s.
 * Customers should implement {@link ResourceReader} to realize their own data storing and fetching logic.
 *
 * As for the underlying "storage later", Stream work-flow does not specify any limitations.
 * Customers can choose to store and fetch the resources from a RDB or NoSQL or whatever, they should realize the way how
 * to get the real value of the {@link Resource} then store it to their data repository, also they should realize the way
 * how to retrieve the real value by {@link ResourceURL} then decorate it into an standard instance of {@link Resource} so that
 * other components in the framework can use it freely.
 */
@Setter
@Getter
public class ResourceCatalog {

    private Cache cache;

    private Map<String, ResourceReader> readers = new HashMap<>();

    /**
     * Register a resource reader instance.
     * @param reader The reader instance needed to register.
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
     * @throws ResourceReadingException ResourceReadingException.
     */
    public Resource readResource(final ResourceURL resourceURL) throws ResourceReadingException {
        Resource result = null;
        if (resourceURL == null || resourceURL.getResourceAuthority() == null) {
            throw new ResourceReadingException("ResourceURL should not be null!");
        }
        if (cache != null && (result = cache.get(resourceURL)) != null) {
            return result;
        }
        ResourceReader reader = resolve(resourceURL);
        if (reader == null) {
            throw new ResourceReadingException(String.format("There is no reader registered for this resourceURL represented resource authority [%s]",
                    resourceURL.getResourceAuthority().getValue()));
        }
        result = reader.read(resourceURL);
        if (cache != null) {
            cache.put(resourceURL, result);
        }
        return result;
    }

    /**
     * Retrieve the resource from the cache speeding up performance.
     * @param resourceURL Resource url.
     * @return Resource.
     * @throws ResourceReadingException ResourceReadingExecption.
     */
    public Resource readResourceFromCache(final ResourceURL resourceURL) throws ResourceReadingException {
        if (cache == null) {
            throw new ResourceReadingException("Cache is not configured!");
        }
        return cache.get(resourceURL);
    }

    /**
     * Test if the resource is expired.
     * @param resource Resource to be tested.
     * @return Checking result.
     * @throws ResourceReadingException ResourceReadingExecption.
     */
    public boolean isResourceExpired(final Resource resource) throws ResourceReadingException {
        if (cache == null) {
            throw new ResourceReadingException("Cache is not configured!");
        }
        return cache.isResourceExpired(resource);
    }

    /**
     * Mark the resource as expired.
     * @param resource Resource be to set as expired.
     * @throws ResourceReadingException ResourceReadingExecption.
     */
    public void setResourceExpired(final Resource resource) throws ResourceReadingException {
        if (cache == null) {
            throw new ResourceReadingException("Cache is not configured!");
        }
        cache.setResourceExpired(resource);
    }

}

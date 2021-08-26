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

package org.stream.core.execution.resource;

import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Cache;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.ResourceURL;

/**
 * A cache implementation using {@link ResourceTank} to store resources.
 * @author weiguanxiong.
 *
 */
public class ResourceTankBasedCache implements Cache {

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource get(final ResourceURL resourceURL) {
        String reference = getReference(resourceURL);
        return WorkFlowContext.resolveResource(reference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final ResourceURL resourceURL, final Resource resource) {
        String refefence = getReference(resourceURL);
        // Cache will help manage the resource reference.
        resource.setResourceReference(refefence);
        WorkFlowContext.attachResource(resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResourceExpired(final Resource resource) {
        // All the resources will be in the memory until the workflow completed.
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceExpired(final Resource resource) {
        String reference = getReference(resource.getResourceURL());
        WorkFlowContext.remove(reference);
    }

    private String getReference(final ResourceURL resourceURL) {
        return "Workflow::" + resourceURL.getResourceAuthority().getValue() + "::" + resourceURL.getPath();
    }
}

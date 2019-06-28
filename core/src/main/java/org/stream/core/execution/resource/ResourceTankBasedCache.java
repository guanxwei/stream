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
        return WorkFlowContext.resolveResource(resourceURL.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final ResourceURL resourceURL, final Resource resource) {
        WorkFlowContext.attachResource(resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResourceExpired(final Resource resource) {
       return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceExpired(final Resource resource) {
        return;
    }

}

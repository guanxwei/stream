package org.stream.core.component;

import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;

public abstract class AsyncActivity extends Activity {

    private ThreadLocal<ResourceTank> resources = new ThreadLocal<ResourceTank>();

    private ThreadLocal<String> primaryResourceReference = new ThreadLocal<String>();

    public void linkUp(final ResourceTank resourceTank, final String primaryResourceReference) { 
        resources.set(resourceTank);
        this.primaryResourceReference.set(primaryResourceReference);
    }

    public void addResource(final Resource resource) {
        resources.get().addResource(resource);
    }

    public void resolveResource(final String resourceReference) {
        resources.get().resolve(resourceReference);
    }

    public Resource getPrimary() {
        if (primaryResourceReference.get() == null) {
            return null;
        }
        return resources.get().resolve(primaryResourceReference.get());
    }

}

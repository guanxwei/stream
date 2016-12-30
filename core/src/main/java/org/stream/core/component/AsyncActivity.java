package org.stream.core.component;

import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;

/**
 * Encapsulation of async activities.
 */
public abstract class AsyncActivity extends Activity {

    private ThreadLocal<ResourceTank> resources = new ThreadLocal<ResourceTank>();

    private ThreadLocal<String> primaryResourceReference = new ThreadLocal<String>();

    /**
     * Link-up the execution work-flow instance's resource tank with this activity instance.
     * Since AsyncActivitys will be executed in other threads, to make these AsyncActivitys
     * be able to retrieve resources from the work-flow or attach back resources to the work-flow
     * instance, this method helps do this job.
     * @param resourceTank The father work-flow instance's resource tank.
     * @param primaryResourceReference The father work-flow instance's primary resource reference.
     */
    public void linkUp(final ResourceTank resourceTank, final String primaryResourceReference) { 
        resources.set(resourceTank);
        this.primaryResourceReference.set(primaryResourceReference);
    }

    /**
     * Attach back resource to the work-flow instance.
     * @param resource The resource that need to be attached to the work-flow instance.
     */
    public void addResource(final Resource resource) {
        resources.get().addResource(resource);
    }

    /**
     * Retrieve a resource entity from the father work-flow instance's resource tank.
     * @param resourceReference The resource reference.
     */
    public void resolveResource(final String resourceReference) {
        resources.get().resolve(resourceReference);
    }

    /**
     * Get the primary resource from the father work-flow instance.
     * @return
     */
    public Resource getPrimary() {
        if (primaryResourceReference.get() == null) {
            return null;
        }
        return resources.get().resolve(primaryResourceReference.get());
    }

    /**
     * Clean the thread local variables so that this thread can be reused by other work-flow instances.
     */
    public void cleanUp() {
        resources.set(null);
        primaryResourceReference.set(null);
    }
}

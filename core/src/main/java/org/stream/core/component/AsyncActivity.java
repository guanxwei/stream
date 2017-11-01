package org.stream.core.component;

import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;

/**
 * Encapsulation of asynchronous activities.
 *
 * These activities will be executed asynchronously.
 */
public abstract class AsyncActivity extends Activity {

    private ThreadLocal<ResourceTank> resources = new ThreadLocal<ResourceTank>();

    private ThreadLocal<String> primaryResourceReference = new ThreadLocal<String>();

    /**
     * Link-up the execution work-flow instance's resource tank with this activity instance.
     * Since AsyncActivitys will be executed in separated threads,
     * we will be no longer able to achieve this by using the methods in {@linkplain WorkFlowContext}.
     * To make these AsyncActivitys be able to retrieve resources from the work-flow or attach back
     * resources to the work-flow instance, work-flow engine will help invoke these method to link-up
     * that resource tank with this activity.
     * Basically, this method may potentially delay the GC to collect unneeded objects, users should keep
     * in mind that the method {@code AsyncActivity#cleanUp()} must be invoked after all the work is done.
     *
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
     * @return Resource entity.
     */
    public Resource resolveResource(final String resourceReference) {
        return resources.get().resolve(resourceReference);
    }

    /**
     * Get the primary resource from the father work-flow instance.
     * @return Primary resource.
     */
    public Resource getPrimary() {
        if (primaryResourceReference.get() == null) {
            return null;
        }
        return resources.get().resolve(primaryResourceReference.get());
    }

    /**
     * Clean the thread local variables so that this thread can be reused by other work-flow instances.
     * And make sure that the unneeded {@linkplain ResourceTank} instance initiated in the main thread is collected.
     */
    public void cleanUp() {
        resources.set(null);
        primaryResourceReference.set(null);
    }
}

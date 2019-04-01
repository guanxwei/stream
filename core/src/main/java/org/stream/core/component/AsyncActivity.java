package org.stream.core.component;

import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;

/**
 * Encapsulation of asynchronous stream work flow activities.
 *
 * All the asynchronous activities will be executed asynchronously alone with the main procedure.
 */
public abstract class AsyncActivity extends Activity {

    private ThreadLocal<ResourceTank> resources = new ThreadLocal<ResourceTank>();

    private ThreadLocal<String> primaryResourceReference = new ThreadLocal<String>();

    /**
     * Link-up the execution work-flow instance's resource tank with this activity instance.
     * Since AsyncActivitys will be executed in separated threads,
     * we will be no longer able to achieve the work flow resources by using the methods in {@link WorkFlowContext}.
     * To make these AsyncActivitys be able to retrieve resources from the work-flow or attach back
     * resources to the work-flow instance, work-flow engine will help invoke these method to link-up
     * the work flow resource tank with the asynchronous-activity.
     * Basically, this method may potentially delay the GC to collect unneeded objects.
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
     * Clean the thread local variables so that the host thread can be reused by other work-flow instances.
     * And make sure that the unneeded {@linkplain ResourceTank} instance initiated in the main thread is collected.
     * 
     * From now on, this method will be automatically invoked after execution, implementations of the {@link AsyncActivity}
     * only need to override the {@link Activity#act()} to do their business actions, all the other things will be done by the
     * execution engine.
     */
    public void cleanUp() {
        resources.set(null);
        primaryResourceReference.set(null);
    }
}

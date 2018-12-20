package org.stream.core.component;

import org.stream.core.resource.ResourceType;

/**
 * Encapsulation of customer specific activity, which will be performed in a specific {@link Node}.
 *
 * Users should extend this interface to customize their own logics and configure them in graph definition files.
 * The work-flow engine will automatically pick up these activities and execute them per graph definition files.
 */
public abstract class Activity {

    /**
     * Perform an activity as part of a work-flow.
     * @return The activity result.
     */
    public abstract ActivityResult act();

    /**
     * Get the name of the activity.
     * @return The activity's name
     */
    public String getActivityName() {
        return getClass().getName();
    }

    /**
     * Get the host node the activity is running on.
     * @return The host node of the activity.
     */
    public Node getExecutionContext() {
        return Node.CURRENT.get();
    }

    /**
     * Get the granted resource type of the graph. Basically, activities should follow the resource type limitation defined in graph definition file,
     * if not unexpected exception may throw.
     * @return The graph's granted primary resource type.
     */
    public ResourceType getGrandtedPrimaryResourceType() {
        return Node.CURRENT.get().getGraph().getResourceType();
    }
}

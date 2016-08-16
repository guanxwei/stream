package org.stream.core.component;

import org.stream.core.resource.ResourceType;

import lombok.Setter;

/**
 * Encapsulation of customer specific activity, which is performed in a specific {@link Node}.
 */
public abstract class Activity {

    @Setter
    private Node node;

    /**
     * Perform an activity as part of a workflow.
     * @return The activity result.
     */
    public abstract ActivityResult act();

    /**
     * Get the name of the activity.
     * @return
     */
    public String getActivityName() {
        return getClass().getName();
    };

    /**
     * Get the node the activity is running on.
     * @return The host node of the activity.
     */
    public Node getExecutionContext() {
        return node;
    };

    /**
     * Get the granted resource type of the graph. Basically, activities should follow the resource type limitation, if not unexpedted exception may throws.
     * @return
     */
    public ResourceType getGrandtedResourceType() {
        return node.getGraph().getResourceType();
    }
}

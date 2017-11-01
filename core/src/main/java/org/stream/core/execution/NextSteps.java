package org.stream.core.execution;

import org.stream.core.component.Node;

import lombok.Setter;

/**
 * Encapsulation of the next step information taken by the {@linkplain Engine}.
 * Each {@link Node} should provide enough information to the {@linkplain Engine} to help determine what to do
 * after completing the work in the current node. In the steam work-flow world, the "work" to be done means the
 * {@linkplain Node} to be executed on.
 *
 * This entity provides such functions to the {@link Engine},
 * it will tell the {@linkplain Engine} which {@linkplain Node} to retrieve and execute
 * based on the {@link ActivityResult} returned by the current {@linkplain Node}.
 *
 */
public class NextSteps {

    @Setter
    private Node success;

    @Setter
    private Node fail;

    @Setter
    private Node suspend;

    /**
     * Return the successor {@link Node}, invoked only when the current {@link Node} returns
     * {@link ActivityResult#SUCCESS}.
     * @return The successor {@link Node}.
     */
    public Node onSuccess() {
        return success;
    }

    /**
     * Return the successor {@link Node}, invoked only when the current {@link Node} returns
     * {@link ActivityResult#FAIL}.
     * @return The successor {@link Node}.
     */
    public Node onFail() {
        return fail;
    }

    /**
     * Return the successor {@link Node}, invoked only when the current {@link Node} returns
     * {@link ActivityResult#SUSPEND}.
     * @return The successor {@link Node}.
     */
    public Node onSuspend() {
        return suspend;
    }

    // CHECKSTYLE:OFF
    public enum NextStepType {
        SUCCESS, FAIL, SUSPEND;
    }
    // CHECKSTYLE:ON
}

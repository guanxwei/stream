package org.stream.extension.observer;

/**
 * Stream framework supported event types. Those events will be triggered during the
 * execution of the workflow procedures. When the applications want to know the real time
 * state of the workflow instances, 
 *
 * @author weiguanxiong
 *
 */
public enum SupportedEvents {

    /**
     * Workflow succeed events.
     */
    SUCCEED("Succeed"),

    /**
     * Workflow suspended events.
     */
    SUSPENDED("Suspended"),

    /**
     * Workflow failed events.
     */
    FAILED("Failed");

    private String type;

    private SupportedEvents(final String type) {
        this.type = type;
    }

    /***
     * Return the name of event type.
     * @return Event type name.
     */
    public String type() {
        return this.type;
    }
}

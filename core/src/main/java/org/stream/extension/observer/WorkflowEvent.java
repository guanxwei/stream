package org.stream.extension.observer;

/**
 * Workflow events arosen when the engine is executing the procedures.
 * @author weigu
 *
 */
public abstract class WorkflowEvent {

    private WorkflowEventContext context;

    /**
     * Event type.
     * @return Event type.
     */
    public abstract String type();

    /**
     * Source that triggers the event.
     * @param context Workflow event context.
     */
    public void context(final WorkflowEventContext context) {
        this.context = context;
    }

    /**
     * Return the event context.
     * @return Source object.
     */
    public WorkflowEventContext getContext() {
        return this.context;
    }
}


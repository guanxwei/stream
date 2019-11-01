package org.stream.extension.observer;

public class WorkflowSuspendEvent extends WorkflowEvent {

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return SupportedEvents.SUSPENDED.type();
    }

}

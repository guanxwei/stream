package org.stream.extension.observer;

public class WorkflowFailedEvent extends WorkflowEvent {

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return SupportedEvents.FAILED.type();
    }

}

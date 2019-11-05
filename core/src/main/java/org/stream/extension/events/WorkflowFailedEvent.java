package org.stream.extension.events;

public class WorkflowFailedEvent extends Event {

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return "Failed";
    }

}

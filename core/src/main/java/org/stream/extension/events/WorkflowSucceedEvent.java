package org.stream.extension.events;

public class WorkflowSucceedEvent extends Event {

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return "Succeed";
    }

}

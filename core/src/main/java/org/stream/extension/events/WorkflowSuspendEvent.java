package org.stream.extension.events;

public class WorkflowSuspendEvent extends Event {

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return "Suspend";
    }

}

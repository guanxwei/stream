package org.stream.extension.events;

/**
 * Workflow initiated event.
 * @author weiguanxiong.
 *
 */
public class WorkflowInitiatedEvent extends Event {

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return "Intiated";
    }

}

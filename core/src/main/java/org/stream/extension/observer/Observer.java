package org.stream.extension.observer;

/**
 * Abstract of functions that will be triggered when something happened during workflow execution procedure.
 * @author weiguanxiong
 *
 */
public interface Observer {

    /**
     * Inform the observer that something interested has happened.
     * @param workflowEvent Work flow event that the observer interested.
     */
    void awake(final WorkflowEvent workflowEvent);

    /**
     * Test if the observer is interested in the event.
     * @param workflowEvent Work flow event.
     * @return <code>true</code> when the event is in the interested list, otherwise <code>false</code>.
     */
    boolean interested(final WorkflowEvent workflowEvent);
}

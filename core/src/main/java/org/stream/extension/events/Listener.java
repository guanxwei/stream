package org.stream.extension.events;

/**
 * Event listener.
 * @author guanxiong wei
 *
 */
public interface Listener {

    /**
     * Process the event.
     * @param event Event to be processed.
     */
    void handle(final Event event);
}

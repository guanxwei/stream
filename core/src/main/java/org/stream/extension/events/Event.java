package org.stream.extension.events;

/**
 * Base class for different kinds of asynchronous events.
 * @param <T> Request.
 * @param <U> Response.
 *
 */
public interface Event<T, U> {

    /**
     * Get source that trigger this event.
     * @return Event source.
     */
    public T getSource();

    /**
     * Get Object that will be sent to the event listener.
     * @return Object attached to the event.
     */
    public U getObject();

    /**
     * Get assigned event id.
     * @return Event id.
     */
    public String getEventID();
}

package org.stream.extension.events;

import org.stream.core.component.Node;

import lombok.Data;

/**
 * Base class to abstract different kinds of asynchronous events.
 *
 */
@Data
public abstract class Event {

    // Object that triggered this event.
    private Object trigger;

    // The time this event was triggered
    private long time;

    // Graph name this event belongs to.
    private String graph;

    // Current node.
    private String node;

    /**
     * Return the type of the event.
     * @return Event type.
     */
    public abstract String type();

    @SuppressWarnings("unchecked")
    public <T> T getTrigger() {
        return (T) this.trigger;
    }

    /**
     * Construct a new instance of event. Concret class type is specified by the input parameter clazz.
     * @param clazz Concret event sub class type.
     * @param trigger Object that triggers this event.
     * @param node Current node.
     * @return New instance of the target event sub-class.
     */
    public static Event of(final Class<? extends Event> clazz, final Object trigger, final Node node) {
        try {
            Event event = clazz.getDeclaredConstructor().newInstance();
            event.setGraph(node.getGraph().getGraphName());
            event.setNode(node.getNodeName());
            event.setTime(System.currentTimeMillis());
            event.setTrigger(trigger);
            return event;
        } catch (Exception e) {
            Event event = new Event() {
                @Override
                public String type() {
                    return "Anony";
                }
            };
            event.setGraph(node.getGraph().getGraphName());
            event.setNode(node.getNodeName());
            event.setTime(System.currentTimeMillis());
            event.setTrigger(trigger);
            return event;
        }

    }
}

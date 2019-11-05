package org.stream.extension.events;

/**
 * Task complete event.
 * @author hzweiguanxiong
 *
 */
public class TaskCompleteEvent extends Event {

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return "Complete";
    }

}

package org.stream.extension.events;

import org.apache.commons.lang3.RandomStringUtils;
import org.stream.extension.meta.Task;

/**
 * Task complete event.
 * @author hzweiguanxiong
 *
 */
public class TaskCompleteEvent implements Event<String, Task> {

    private String source;
    private Task object;
    private String eventID;

    /**
     * Only used for Jasonfy functions.
     */
    public TaskCompleteEvent() { }

    /**
     * Constructor.
     * @param source Event source.
     * @param object Task.
     */
    public TaskCompleteEvent(final String source, final Task object) {
        this.source = source;
        this.object = object;
        this.eventID = RandomStringUtils.randomAlphabetic(20);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSource() {
        return source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Task getObject() {
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEventID() {
        return eventID;
    }

}

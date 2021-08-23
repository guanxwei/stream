package org.stream.extension.persist;

import java.util.Collections;
import java.util.List;

import org.stream.extension.events.Event;
import org.stream.extension.events.EventCenter;
import org.stream.extension.events.TaskCompleteEvent;
import org.stream.extension.meta.Task;

import lombok.Setter;

/**
 * Kafka based implementation of {@linkplain TaskStorage}.
 * Will push the entity to the kafka message queue so that anybody that is interested in can process the data.
 * @author guanxiong wei
 *
 */
public class KafkaBasedTaskStorage implements TaskStorage {

    @Setter
    private EventCenter eventCenter;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean persist(final Task task) {
        eventCenter.fireEvent(Event.of(TaskCompleteEvent.class, task, null));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(final Task task) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Task query(final String taskID) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Task> queryStuckTasks() {
        return Collections.emptyList();
    }

}

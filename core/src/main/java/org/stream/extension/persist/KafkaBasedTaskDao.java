package org.stream.extension.persist;

import org.stream.extension.events.EventCenter;
import org.stream.extension.events.TaskCompleteEvent;
import org.stream.extension.meta.Task;

import lombok.Setter;

/**
 * Kafka based implementation of {@linkplain TaskDao}.
 * Will push the entity to the kafka message queue so that anybody that is interested in can process the data.
 * @author hzweiguanxiong
 *
 */
public class KafkaBasedTaskDao implements TaskDao {

    @Setter
    private EventCenter eventCenter;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean persist(final Task task) {
        eventCenter.fireEvent(new TaskCompleteEvent(this.getClass().getSimpleName(), task));
        return true;
    }

}

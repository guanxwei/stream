package org.stream.extension.persist;

import org.stream.extension.events.Event;
import org.stream.extension.events.Listener;
import org.stream.extension.events.TaskCompleteEvent;
import org.stream.extension.meta.Task;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * MangoDB event listener.
 * Default worker to back-up completed tasks.
 *
 */
@Slf4j
public class MangoDBBasedTaskCompleteListener implements Listener {

    @Setter
    private TaskStorage taskStorage;

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final Event event) {
        TaskCompleteEvent realEvent = (TaskCompleteEvent) event;
        Task task = realEvent.getTrigger();
        log.info("Save completed task [{}]", task.getTaskId());

        taskStorage.persist(task);
    }

}

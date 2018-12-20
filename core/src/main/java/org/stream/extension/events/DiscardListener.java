package org.stream.extension.events;

import org.stream.extension.meta.Task;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiscardListener implements Listener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final Event<?, ?> event) {
        TaskCompleteEvent realEvent = (TaskCompleteEvent) event;
        Task task = realEvent.getObject();
        log.info("Save completed task [{}], content", task.getTaskId(), task.toString());
    }

}


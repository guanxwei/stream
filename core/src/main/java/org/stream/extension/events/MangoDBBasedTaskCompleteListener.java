package org.stream.extension.events;

import org.stream.extension.clients.MongoClient;
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
    private MongoClient mongoClient;

    @Setter
    private String collectionName;

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final Event event) {
        TaskCompleteEvent realEvent = (TaskCompleteEvent) event;
        Task task = (Task) realEvent.getTrigger();
        log.info("Save completed task [{}]", task.getTaskId());

        mongoClient.save(task.getTaskId(), task, collectionName);
    }

}

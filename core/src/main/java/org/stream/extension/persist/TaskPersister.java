package org.stream.extension.persist;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStep;

/**
 * Encapsulation of Task persister.
 * @author guanxiong wei
 *
 */
public interface TaskPersister {

    /**
     * Persist task detail in somewhere permanently.
     * @param task Task to be saved.
     * @return Manipulation result.
     */
    boolean persist(final Task task);

    /**
     * Get a task by key.
     * @param key Task's unique key.
     * @return Jsonfied task entity.
     */
    String get(final String key);

    /**
     * Get pending on retry tasks from Redis.
     * @param type List type.
     *         1 : retry list
     *         2 : back-up list
     * @param queue The target queue.
     * @return Task list.
     */
    Collection<String> getPendingList(final int type, final int queue);

    /**
     * Try to lock the task Id to avoid contention.
     * @param taskId Task id to be locked.
     * @return Manipulation result.
     */
    boolean tryLock(final String taskId);

    /**
     * Release the lock we have locked.
     * @param taskId Task id to be released.
     * @return Manipulation result.
     */
    boolean releaseLock(final String taskId);

    /**
     * Grab the distribute lock of the task so that only one runner can process the task.
     * If the target task is not initiated yet in db, create a new record in the db table;
     * if it is initiated, update the task status. A new task step record will always be added.
     * @param task Task to initiated or updated.
     * @param withInsert {@code true} insert new task in the db, otherwise update task in db.
     * @param taskStep Task step detail.
     * @return Manipulation result.
     */
    boolean initiateOrUpdateTask(final Task task, final boolean withInsert, final TaskStep taskStep);

    /**
     * Remove the hub since the job is completely done.
     * @param taskId Task id.
     * @return Manipulation result.
     */
    boolean removeHub(final String taskId);

    /**
     * Set the application name, the application's name should be unique.
     * @param application Application name.
     */
    void setApplication(final String application);

    /**
     * Mark the task as suspended.
     * @param task Task to be suspended.
     * @param time Time interval in {@link TimeUnit#MILLISECONDS}.
     * @param taskStep Task step detail.
     */
    void suspend(final Task task, final double time, final TaskStep taskStep);

    /**
     * Mark the task as completed.
     * @param task Task which is completed.
     */
    void complete(final Task task);

    /**
     * Making the task persister working in debug mode.
     * @param debug Flag to make the persister work in debug mode.
     */
    void setDebug(final boolean debug);

    /**
     * Retrieve the latest transfer data.
     * @param taskId Task id.
     * @return The latest transfer data.
     */
    StreamTransferData retrieveData(final String taskId);

    /**
     * Retrieve stuck tasks from the storage.
     * @return Stuck task list.
     */
    List<Task> retrieveStuckTasksFromDB();

    /**
     * Return queues the tasks are persisted, the main reason why the storage is divided into several queues
     * is to speed up processing.
     * @return Queues the tasks are persisted
     */
    int getQueues();

    /**
     * Get the application name, the application's name should be unique.
     * @return Application name.
     */
    String getApplication();

    /**
     * Test if the task id being processed by this machine.
     * @param taskId Target task id.
     * @return {@code true} task is being processed should yield, otherwise {@code false} should continue.
     */
    boolean isProcessing(final String taskId);
}

package org.stream.extension.persist;

import java.util.Collection;

import org.stream.extension.meta.Task;

/**
 * Encapsulation of Task persister.
 * @author hzweiguanxiong
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
     * Mark a task as pending on retry status in Redis.
     * @param task Task to marked.
     * @param pattern Retry pattern execution pattern.
     * @param time Expire time.
     * @return Manipulation result.
     */
    boolean mark(final Task task, final String pattern, final int time);

    /**
     * Mark a task as processed and remove its record in Redis.
     * @param task Task to be unmarked.
     * @return Manipulation result.
     */
    boolean unmark(final Task task);

    /**
     * Get a task by key.
     * @param key Task's Redis key.
     * @return Manipulation result.
     */
    String get(final String key);

    /**
     * Get pending on retry tasks from Redis.
     * @param type List type.
     *         1 : retry list
     *         2 : back-up list
     * @return Task list.
     */
    Collection<String> getPendingList(final int type);

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
     * Save an back up in DB in case the server crashes.
     * @param taskId Task id.
     * @param content Passby content.
     * @return Manipulation result.
     */
    boolean setHub(final String taskId, final String content);

    /**
     * Remove the hub since the job is completely done.
     * @param taskId Task id.
     * @return Manipulation result.
     */
    boolean removeHub(final String taskId);

    /**
     * Update expire time.
     * @param taskId Task id.
     * @return Operation result.
     */
    boolean updateTime(final String taskId);

    /**
     * Set the application name, the application's name should be unique.
     * @param application Application name.
     */
    void setApplication(final String application);

    /**
     * Mark the task as suspended.
     * @param task Task to be suspended.
     * @param time Time window.
     */
    void suspend(final Task task, final int time);

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
}

package org.stream.extension.persist;

import java.util.List;

import org.stream.extension.meta.Task;

/**
 * Task data access layer object provides data access methods like saving a new task
 * in db, updating a task's status .etc.
 * @author hzweiguanxiong
 *
 */
public interface TaskStorage {

    /**
     * Save task in persistent layer.
     * @param task Task to be saved.
     * @return Manipulation result.
     */
    boolean persist(final Task task);

    /**
     * Update task in persistent layer.
     * @param task Task to be updated.
     * @return {@code true} update successfully, otherwise {@code false}.
     */
    boolean update(final Task task);

    /**
     * Query task by task id.
     * @param taskID Task id.
     * @return Task entity.
     */
    Task query(final String taskID);

    /**
     * Query stuck tasks.
     * @return Stuck tasks.
     */
    List<Task> queryStuckTasks();
}

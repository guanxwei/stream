package org.stream.extension.persist;

import java.util.List;

import org.stream.extension.meta.TaskStep;

/**
 * Task step data access layer object provides data access methods like
 * saving a new task step in db, query task steps by task id .etc.
 * @author guanxiong wei
 *
 */
public interface TaskStepStorage {

    /**
     * Insert a new step instance in DB.
     * @param taskStep New task step to be saved.
     * @return {@code true} succeed {@code false} failed.
     */
    boolean insert(final TaskStep taskStep);

    /**
     * Get the target task's step list.
     * @param taskId Targe task's identity.
     * @return List of task steps belongs to the target task.
     */
    List<TaskStep> getByTaskId(final String taskId);

    /**
     * Get the target task's latest step entity.
     * @param taskId Targe task's identity.
     * @return The latest step of the task.
     */
    TaskStep getLatestStep(final String taskId);
}

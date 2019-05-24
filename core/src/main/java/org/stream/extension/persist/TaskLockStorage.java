package org.stream.extension.persist;

import org.stream.extension.meta.TaskLock;

/**
 * Task lock data access layer object.
 * @author hzweiguanxiong
 *
 */
public interface TaskLockStorage {

    /**
     * Initiate the task lock.
     * @param taskLock Task lock to be initiated.
     * @return 1 task locked added, 0 failed.
     */
    int initiate(final TaskLock taskLock);

    /**
     * Try to lock the task.
     * @param taskId Task id.
     * @param version Task version.
     * @return <code>true</code> lock grabbed, otherwise <code>false</code>.
     */
    boolean tryLock(final String taskId, final int version);
}

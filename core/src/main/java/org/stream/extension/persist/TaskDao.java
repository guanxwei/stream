package org.stream.extension.persist;

import org.stream.extension.meta.Task;

/**
 * Task DAO used to store task information in DB.
 * @author hzweiguanxiong
 *
 */
public interface TaskDao {

    /**
     * Save task in persistent layer.
     * @param task Task to be saved.
     * @return Manipulation result.
     */
    boolean persist(final Task task);

}

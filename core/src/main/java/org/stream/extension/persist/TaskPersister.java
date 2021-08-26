/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stream.extension.persist;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.stream.core.component.Node;
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
     * @param current Current node that this task is stuck at.
     */
    void suspend(final Task task, final double time, final TaskStep taskStep, final Node current);

    /**
     * Mark the task as completed after execute the activity in node.
     * @param task Task which is completed.
     * @param node Final node the task is executed.
     */
    void complete(final Task task, final Node node);

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

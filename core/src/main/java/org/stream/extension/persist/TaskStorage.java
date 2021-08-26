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

import org.stream.extension.meta.Task;

/**
 * Task data access layer object provides data access methods like saving a new task
 * in db, updating a task's status .etc.
 * @author guanxiong wei
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

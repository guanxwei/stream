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

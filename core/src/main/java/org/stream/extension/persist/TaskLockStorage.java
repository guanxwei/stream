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

import org.stream.extension.meta.TaskLock;

/**
 * Task lock data access layer object.
 * @author guanxiong wei
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

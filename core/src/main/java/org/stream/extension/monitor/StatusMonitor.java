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

package org.stream.extension.monitor;

import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.persist.QueueHelper;
import org.stream.extension.persist.RedisService;

import lombok.Getter;
import lombok.Setter;

/**
 * Task executor status monitor.
 * @author guanxiong wei
 *
 */
public class StatusMonitor {

    @Setter @Getter
    private TaskExecutor taskExecutor;

    @Setter
    private RedisService redisService;

    /**
     * Get active task number that being executed by the work flow engine.
     * @return Active task number.
     */
    public int activeTasks() {
        return taskExecutor.getActiveTasks();
    }

    /**
     * Get task number that being waiting to be executed by the work flow engine;
     * @return Queued task number.
     */
    public int getQueuedTasks() {
        return taskExecutor.getQueuedTasks();
    }

    /**
     * Get thread pool size.
     * @return Return thread pool size.
     */
    public int getPoolSize() {
        return taskExecutor.getPoolSize();
    }

    /**
     * Get the quantity of suspended tasks.
     * @param application Application name.
     * @return The quantity of suspended tasks.
     */
    public long getSuspendedTaks(final String application) {
        long sum = 0;
        for (int i = 0; i < QueueHelper.DEFAULT_QUEUES; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(QueueHelper.RETRY_KEY).append(application).append("_").append(i);
            String queue = sb.toString();
            sum += redisService.getListSize(queue);
        }

        return sum;
    }
}

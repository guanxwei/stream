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

package org.stream.core.execution;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;
import org.stream.core.helper.Jackson;
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.meta.Task;
import org.stream.extension.persist.DelayQueue;
import org.stream.extension.persist.FifoQueue;
import org.stream.extension.persist.QueueHelper;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.persist.TaskStorage;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel watching the tasks' execute status.
 * @author guanxiongwei
 *
 */
@Slf4j
public class Sentinel {

    @Setter
    private TaskExecutor taskExecutor;
    @Setter
    private TaskPersister taskPersister;
    @Setter
    private DelayQueue delayQueue;
    @Setter
    private FifoQueue fifoQueue;
    @Setter
    private TaskStorage taskStorage;

    private volatile boolean shutdown = false;

    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);

    /**
     * Initiation method to prepare back-end workers to process pending on retry work-flow instances.
     */
    public void init() {
        initiate(1, 1000);
        initiate(2, 5000);
        initiate(3, 30000);
        taskExecutor.shutDownHook();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown = true;
        }));
    }

    private void initiate(final int type, final int time) {
        int threads = getQueues(type);
        for (int i = 0; i < threads; i++) {
            Integer queue = i;
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                String queueName = QueueHelper.getQueueNameFromIndex(QueueHelper.getPrefix(type),
                        taskPersister.getApplication(), queue);
                if (!shutdown) {
                    List<String> contents = new LinkedList<>();
                    contents.addAll(getPendingList(type, queue));
                    if (!contents.isEmpty()) {
                        log.info("Pending tasks [{}] loaded for type [{}] from queue [{}]", Jackson.json(contents), type,
                                queueName);
                        for (String id : contents) {
                            log.info("Submit retry job for task [{}]", id);
                            taskExecutor.retry(id);
                        }
                    }
                }
            }, 3000, time, TimeUnit.MILLISECONDS);
        }
    }

    private int getQueues(final int type) {
        if (type == 3) {
            return 1;
        }
        if (type == 1 || type == 2) {
            return QueueHelper.DEFAULT_QUEUES;
        }
        return 0;
    }

    public Collection<String> getPendingList(final int type, final int queue) {

        assert taskPersister.getApplication() != null;

        String queueName = QueueHelper.getQueueNameFromIndex(QueueHelper.getPrefix(type), taskPersister.getApplication(), queue);
        Collection<String> result = Collections.emptyList();
        try {
            switch (type) {
            case 1:
                result = delayQueue.getItems(queueName, System.currentTimeMillis());
                break;
            case 2:
                result = fifoQueue.pop(queueName, 10);
                break;
            case 3:
                List<Task> tasks = taskStorage.queryStuckTasks();
                if (!CollectionUtils.isEmpty(tasks)) {
                    result = tasks.parallelStream()
                            .map(Task::getTaskId)
                            .collect(Collectors.toList());
                }
                break;
            default:
                break;
            }

            return result;
        } catch (Exception e) {
            log.warn("Fail to load pending tasks for type [{}]", type, e);
        }

        return Collections.emptyList();
    }
}

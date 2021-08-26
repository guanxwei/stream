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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
import org.stream.core.component.Node;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.extension.clients.RedisClient;
import org.stream.extension.events.Event;
import org.stream.extension.events.EventCenter;
import org.stream.extension.events.EventsHelper;
import org.stream.extension.events.WorkflowFailedEvent;
import org.stream.extension.events.WorkflowSucceedEvent;
import org.stream.extension.events.WorkflowSuspendEvent;
import org.stream.extension.io.HessianIOSerializer;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStatus;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.settings.Settings;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link TaskPersister}.
 * Users should themselves implements {@link TaskStepStorage}, 
 * {@link TaskStepStorage}, {@link RedisService} and inject the implementations into this
 * object if they want to use this default implementation.
 *
 * This implement uses redis cluster to speed up the work-flows's processing procedure.
 * Users should prepare a redis cluster in real environment, if not, please implement {@link TaskPersister}
 * and use that version.
 * @author guanxiong wei
 *
 */
@Slf4j
public class TaskPersisterImpl implements TaskPersister {

    private Map<String, String> processingTasks = new HashMap<>();
    private Map<String, Long> lockingTimes = new HashMap<>();

    // Default use a mock client, do nothing.
    @Setter
    private TaskStorage messageQueueBasedTaskStorage = Mockito.mock(TaskStorage.class);

    @Setter
    private TaskStorage taskStorage;

    @Setter
    private TaskStepStorage taskStepStorage;

    @Setter
    private RedisClient redisClient;

    @Setter
    private DelayQueue delayQueue;

    @Setter
    private FifoQueue fifoQueue;

    @Setter
    private boolean debug = false;

    @Getter
    private String application;

    @Setter
    private EventCenter eventCenter;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean persist(final Task task) {
        return taskStorage.update(task) && messageQueueBasedTaskStorage.persist(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String get(final String key) {
        Task task = taskStorage.query(key);

        if (task != null) {
            return task.toString();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryLock(final String taskId) {
        long current = System.currentTimeMillis();
        boolean ownered = Thread.currentThread().getName().equals(processingTasks.get(taskId));

        // The lock was grabbed by this thread in the previous step, just skip the procedure or refresh the lock time.
        if (ownered && current - lockingTimes.get(taskId) < Settings.LOCK_EXPIRE_TIME) {
            if (current - lockingTimes.get(taskId) > Settings.LOCK_EXPIRE_TIME / 2) {
                // refresh locked time if we have hold the lock for a long time
                String expectedValue = genLockValue(lockingTimes.get(taskId));
                boolean refreshed = redisClient.updateKeyExpireTimeIfMatch(genLock(taskId), expectedValue);
                if (refreshed) {
                    lockingTimes.put(taskId, current);
                    log.info("Lock info refreshed");
                } else {
                    log.warn("Fail refreshing the lock expire time, please ");
                    return false;
                }
            }

            return true;
        }

        // The locked was grabbed by another thread in the same JVM.
        if (isProcessing(taskId) && !ownered) {
            log.info("Another thread in the jvm is processing the task, skip");
            // Duplicate thread in the same host.
            long lockingTime = lockingTimes.get(taskId);
            if (System.currentTimeMillis() - lockingTime > Settings.LOCK_EXPIRE_TIME) {
                // The owner thread must be crashed or stuck, and the lock must be expired or refreshed by other workers.
                // Try to grab the lock, if succeed, kick off the previous owner.
                log.warn("The processing thread must be crashed or blocked by some actions, will try to grab the lock");
                return requireLock(taskId, current);
            }
            return false;
        }

        return requireLock(taskId, current);
    }

    private boolean requireLock(final String taskId, final long currentTime) {
        String threadName = Thread.currentThread().getName();
        log.info("Try to grab the lock for task [{}] at time [{}] by thread [{}]", taskId, currentTime, threadName);
        boolean locked = redisClient.setnxWithExpireTime(genLock(taskId), genLockValue(currentTime)) == 1L;

        if (locked) {
            log.info("Thread [{}] Grab the lock for task [{}]", Thread.currentThread().getName(), taskId);
            markAsLocked(taskId, currentTime);
        }

        return locked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean releaseLock(final String taskId) {
        processingTasks.remove(taskId);
        Long lockTime = lockingTimes.remove(taskId);
        // Lock for to long time, let other workers to release the lock.
        if (lockTime != null && System.currentTimeMillis() - lockTime > Settings.LOCK_EXPIRE_TIME) {
            return true;
        }

        return redisClient.del(genLock(taskId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean initiateOrUpdateTask(final Task task, final boolean withInsert, final TaskStep taskStep) {
        // Refresh lock time.
        if (isLegibleOwner(genLock(task.getTaskId()))) {
            if (withInsert) {
                return taskStepStorage.insert(taskStep) && taskStorage.persist(task);
            } else {
                return taskStepStorage.insert(taskStep) && taskStorage.update(task);
            }
        } else {
            throw new WorkFlowExecutionExeception("Lock has been grabed by other processors, give up execution");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeHub(final String taskId) {
        assert application != null;

        processingTasks.remove(taskId);
        lockingTimes.remove(taskId);
        delayQueue.deleteItem(QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, taskId), taskId);
        return fifoQueue.remove(QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, taskId), taskId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplication(String application) {
        this.application = application;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspend(final Task task, final double time, final TaskStep taskStep, final Node current) {
        log.info("Suspend task [{}] at node [{}]", task.getTaskId(), task.getNodeName());
        taskStorage.update(task);
        taskStepStorage.insert(taskStep);
        double score = System.currentTimeMillis() + time;
        if (debug) {
            // To make sure Unit test cases can be run quickly.
            score = 5;
        }
        delayQueue.enqueue(QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, task.getTaskId()), task.getTaskId(), score);
        log.info("Task [{}] pushed to delay queue [{}]", task.getTaskId(),
                QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, task.getTaskId()));
        fifoQueue.remove(QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, task.getTaskId()), task.getTaskId());
        releaseLock(task.getTaskId());
        if (task.getRetryTimes() == 3 || task.getRetryTimes() == 10) {
            EventsHelper.fireEvent(eventCenter, Event.of(WorkflowSuspendEvent.class, task.getTaskId(), current), false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete(final Task task, final Node node) {
        if (task.getStatus() == TaskStatus.FAILED.code()) {
            EventsHelper.fireEvent(eventCenter, Event.of(WorkflowFailedEvent.class, task.getTaskId(), node), false);
        } else {
            EventsHelper.fireEvent(eventCenter, Event.of(WorkflowSucceedEvent.class, task.getTaskId(), node), false);
        }
        delayQueue.deleteItem(QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, task.getTaskId()), task.getTaskId());
        fifoQueue.remove(QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, task.getTaskId()), task.getTaskId());
        releaseLock(task.getTaskId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamTransferData retrieveData(final String taskId) {
        TaskStep taskStep = taskStepStorage.getLatestStep(taskId);
        return HessianIOSerializer.decode(taskStep.getStreamTransferData(), StreamTransferData.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Task> retrieveStuckTasksFromDB() {
        return taskStorage.queryStuckTasks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isProcessing(final String taskId) {
        return processingTasks.containsKey(taskId);
    }

    private void markAsLocked(final String taskId, final long current) {
        lockingTimes.put(taskId, current);
        processingTasks.put(taskId, Thread.currentThread().getName());
        fifoQueue.push(QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, taskId), taskId);
        delayQueue.deleteItem(QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, taskId), taskId);
    }

    private String genLock(final String taskId) {
        return taskId + "_lock";
    }

    private String genLockValue(final long current) {
        return Settings.HOST_NAME + "_" + current;
    }

    private boolean isLegibleOwner(final String lock) {
        String ownerInfo = redisClient.get(lock);
        if (ownerInfo == null) {
            return true;
        }

        return ownerInfo.startsWith(Settings.HOST_NAME);
    }
}

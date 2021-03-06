package org.stream.extension.persist;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.CollectionUtils;
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

    private static final String HOST_NAME = RandomStringUtils.randomAlphabetic(32);
    private Map<String, String> processingTasks = new HashMap<>();
    private Map<String, Long> lockingTimes = new HashMap<>();
    // Lock expire time in milliseconds.
    public static final int LOCK_EXPIRE_TIME = 6 * 1000;

    @Setter
    private TaskStorage messageQueueBasedTaskStorage;

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
    public Collection<String> getPendingList(final int type, final int queue) {
        assert application != null;

        String queueName = QueueHelper.getQueueNameFromIndex(QueueHelper.getPrefix(type), application, queue);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryLock(final String taskId) {
        long current = System.currentTimeMillis();
        boolean ownered = Thread.currentThread().getName().equals(processingTasks.get(taskId));

        if (ownered && current - lockingTimes.get(taskId) < LOCK_EXPIRE_TIME) {
            if (current - lockingTimes.get(taskId) > LOCK_EXPIRE_TIME / 2 && isLegibleOwner(genLock(taskId))) {
                // refresh locked time if we have hold the lock for a long time
                redisClient.setWithExpireTime(genLock(taskId), genLockValue(current), LOCK_EXPIRE_TIME / 1000);
                lockingTimes.put(taskId, current);
                log.info("Lock info refreshed");
            }

            return true;
        }

        if (isProcessing(taskId) && !ownered) {
            log.info("Another thread in the jvm is processing the task, skip");
            // Duplicate thread in the same host.
            long lockingTime = lockingTimes.get(taskId);
            if (System.currentTimeMillis() - lockingTime > LOCK_EXPIRE_TIME) {
                // The owner thread must be crashed or stuck.
                releaseLock(taskId);
            }
            return false;
        }

        boolean locked = redisClient.setnx(genLock(taskId), genLockValue(current)) == 1L;

        if (locked) {
            log.info("Mark as locked");
            markAsLocked(taskId, current);
            return true;
        } else {
            String value = redisClient.get(genLock(taskId));
            long lockedTime = parseLock(value);
            if (current - lockedTime >= LOCK_EXPIRE_TIME) {
                log.info("Release stuck lock");
                releaseLock(taskId);
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean releaseLock(final String taskId) {
        processingTasks.remove(taskId);
        Long lockTime = lockingTimes.remove(taskId);
        // Lock for to long time, let other workers to release the lock.
        if (lockTime != null && System.currentTimeMillis() - lockTime > LOCK_EXPIRE_TIME) {
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
    public void suspend(final Task task, final double time, final TaskStep taskStep) {
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
            EventsHelper.fireEvent(eventCenter, Event.of(WorkflowSuspendEvent.class, task.getTaskId(), Node.CURRENT.get()), false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete(final Task task) {
        if (task.getStatus() == TaskStatus.FAILED.code()) {
            EventsHelper.fireEvent(eventCenter, Event.of(WorkflowFailedEvent.class, task.getTaskId(), Node.CURRENT.get()), false);
        } else {
            EventsHelper.fireEvent(eventCenter, Event.of(WorkflowSucceedEvent.class, task.getTaskId(), Node.CURRENT.get()), false);
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
    public int getQueues() {
        return QueueHelper.DEFAULT_QUEUES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isProcessing(final String taskId) {
        // If the lock has been locked for a long period of time, ingore the previous locking owner.
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
        return HOST_NAME + "_" + current;
    }

    private boolean isLegibleOwner(final String lock) {
        String ownerInfo = redisClient.get(lock);
        if (ownerInfo == null) {
            return true;
        }

        return ownerInfo.startsWith(HOST_NAME);

    }

    private long parseLock(final String ownerInfo) {
        if (ownerInfo == null) {
            return 0L;
        }
        String[] infos = ownerInfo.split("_");
        return Long.valueOf(infos[1]);
    }

}

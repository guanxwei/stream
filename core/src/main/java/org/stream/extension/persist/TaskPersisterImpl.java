package org.stream.extension.persist;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.CollectionUtils;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.extension.clients.RedisClient;
import org.stream.extension.io.HessianIOSerializer;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
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
 * @author hzweiguanxiong
 *
 */
@Slf4j
public class TaskPersisterImpl implements TaskPersister {

    private static final String HOST_NAME = RandomStringUtils.randomAlphabetic(32);
    // Lock expire time in milliseconds.
    private static final int LOCK_EXPIRE_TIME = 6 * 1000;
    private Map<String, String> processingTasks = new HashMap<String, String>();

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
                if (lockQueue(queueName)) {
                    result = delayQueue.getItems(queueName, System.currentTimeMillis());
                }
                break;
            case 2:
                if (lockQueue(queueName)) {
                    result = fifoQueue.pop(queueName, 10);
                }
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
        boolean locked = redisClient.setnx(genLock(taskId), genLockValue()) == 1L;
        if (locked) {
            markAsLocked(taskId);
            return true;
        } else if (isLegibleOwner(genLock(taskId))) {
            if (processingTasks.containsKey(taskId)
                    && !processingTasks.get(taskId).equals(Thread.currentThread().getName())) {
                return false;
            }
            // refresh locked time.
            redisClient.set(genLock(taskId), genLockValue());
            processingTasks.put(taskId, Thread.currentThread().getName());
            return true;
        } else {
            releaseExpiredLock(genLock(taskId), LOCK_EXPIRE_TIME);
            processingTasks.remove(taskId);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean releaseLock(final String taskId) {
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
        log.info("Task [{}] pushed to delay queue [{}]", QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, task.getTaskId()));
        fifoQueue.remove(QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, task.getTaskId()), task.getTaskId());
        releaseLock(task.getTaskId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete(final Task task) {
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
        return processingTasks.containsKey(taskId);
    }

    private void markAsLocked(final String taskId) {
        processingTasks.put(taskId, Thread.currentThread().getName());
        fifoQueue.push(QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, taskId), taskId);
        delayQueue.deleteItem(QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, taskId), taskId);
    }

    private String genLock(final String taskId) {
        return taskId + "_lock";
    }

    private String genLockValue() {
        return HOST_NAME + "_" + System.currentTimeMillis();
    }

    private boolean isLegibleOwner(final String lock) {
        String ownerInfo = redisClient.get(lock);
        if (ownerInfo == null) {
            return true;
        }

        if (ownerInfo.startsWith(HOST_NAME)) {
            return true;
        }

        return false;
    }

    private long parseLock(final String ownerInfo) {
        if (ownerInfo == null) {
            return 0L;
        }
        String[] infos = ownerInfo.split("_");
        return Long.valueOf(infos[1]);
    }

    private boolean lockQueue(final String queueName) {
        String lock = queueName + "::lock";
        String value = genLockValue();
        boolean locked = redisClient.setnx(lock, value) == 1L;
        if (locked) {
            redisClient.setWithExpireTime(lock, value, 1);
        } else if (isLegibleOwner(lock)) {
            redisClient.setWithExpireTime(lock, value, 1);
        } else {
            releaseExpiredLock(lock, 1000);
        }

        return locked;
    }

    private void releaseExpiredLock(final String lock, final int seconds) {
        // Someone else owns the lock, we should check if it has expired.
        long lockTime = parseLock(redisClient.get(lock));
        long now = System.currentTimeMillis();
        /** 
         * If the lock was hold longer then 6 seconds, we would think that the previous owner has crashed.
         * In most cases a remote call should be done in at most 3 seconds, here set the lock as double the
         * most used timeout * 2 seconds.
         */
        if (now - lockTime >= seconds) {
            redisClient.del(lock);
        }
    }
}

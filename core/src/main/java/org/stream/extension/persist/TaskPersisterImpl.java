package org.stream.extension.persist;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.CollectionUtils;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStep;

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

    @Setter
    private TaskStorage messageQueueBasedTaskStorage;

    @Setter
    private TaskStorage taskStorage;

    @Setter
    private TaskStepStorage taskStepStorage;

    @Setter
    private RedisService redisService;

    @Setter
    private DelayQueue delayQueue;

    private String application;

    @Setter
    private boolean debug = false;

    private static final String HOST_NAME = RandomStringUtils.randomAlphabetic(32);

    // Lock expire time in milliseconds.
    private static final long LOCK_EXPIRE_TIME = 10 * 1000;

    public static final int DEFAULT_QUEUES = 8;
    public static final String RETRY_KEY = "stream_auto_scheduled_retry_set_";
    public static final String BACKUP_KEY = "stream_auto_scheduled_backup_set_";

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean persist(final Task task) {
        /**
         * Persist the task in reliable storage and send a message to Kafka cluster if configured. 
         */
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

        Collection<String> result = Collections.emptyList();
        try {
            switch (type) {
            case 1:
                result = delayQueue.getItems(getQueueName(RETRY_KEY, application, queue), Double.valueOf("0"),
                        System.currentTimeMillis());
                break;
            case 2:
                result = redisService.lrange(getQueueName(BACKUP_KEY, application, queue), 0, 10);
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
        boolean result = redisService.setnx(taskId + "_lock", HOST_NAME + "_" + System.currentTimeMillis()) == 1L;
        if (result) {
            // No body else grabs the lock, we are the winner of contest;
            // Mark as locked.
            markAsLocked(taskId);
            return true;
        } else if (isLegibleOwner(taskId)) {
            // Retry within the legible time window, return true directly and refresh the lock time.
            redisService.set(taskId + "_lock", HOST_NAME + "_" + System.currentTimeMillis());
            return true;
        } else {
            // Someone else owns the lock, we should check if it has expired.
            long lockTime = parseLock(redisService.get(taskId + "_lock"));
            long now = System.currentTimeMillis();
            /** 
             * If the lock was hold longer then 10 seconds, we would think that the previous owner has crashed.
             * In most cases a remote call should be done in at most 3 seconds, here set the lock as triple the
             * most used timeout * 3 + 1 seconds.
             */
            if (now - lockTime >= LOCK_EXPIRE_TIME) {
                /**
                 * The task has been locked too long, release the lock so as other contenders have chances to retry the work-flow.
                 */
                redisService.del(taskId + "_lock");
            }
            return false;
        }
    }

    private void markAsLocked(final String taskId) {
        redisService.lrem(getQueueName(BACKUP_KEY, application, taskId), 0, taskId);
        // Add the task id at the tail of the back up queue.
        redisService.rpush(getQueueName(BACKUP_KEY, application, taskId), taskId);
        // Remove the task from the retry queue.
        redisService.zdel(getQueueName(RETRY_KEY, application, taskId), taskId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean releaseLock(final String taskId) {
        return redisService.del(taskId + "_lock");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setHub(final String taskId, final Task content, final boolean withInsert, final TaskStep taskStep) {
        // Refresh lock time.
        if (isLegibleOwner(taskId)) {
            if (withInsert) {
                return taskStepStorage.insert(taskStep) && taskStorage.persist(content);
            } else {
                return taskStepStorage.insert(taskStep) && taskStorage.update(content);
            }
        } else {
            throw new WorkFlowExecutionExeception("Lock has been grabed by other processors, give up execution");
        }

    }

    private boolean isLegibleOwner(final String taskId) {
        String ownerInfo = redisService.get(taskId + "_lock");
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeHub(final String taskId) {
        assert application != null;

        redisService.zdel(getQueueName(RETRY_KEY, application, taskId), taskId);
        return redisService.lrem(getQueueName(BACKUP_KEY, application, taskId), 0, taskId);
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
        redisService.zadd(getQueueName(RETRY_KEY, application, task.getTaskId()), task.getTaskId(), score);
        redisService.lrem(getQueueName(BACKUP_KEY, application, task.getTaskId()), 0, task.getTaskId());
        redisService.del(task.getTaskId() + "_lock");
        log.info("Task updated to [{}]", task.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete(final Task task) {
        redisService.del(task.getTaskId());
        redisService.del(task.getTaskId() + "_lock");
        redisService.zdel(getQueueName(RETRY_KEY, application, task.getTaskId()), task.getTaskId());
        redisService.lrem(getQueueName(BACKUP_KEY, application, task.getTaskId()), 1, task.getTaskId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamTransferData retrieveData(final String taskId) {
        TaskStep taskStep = taskStepStorage.getLatestStep(taskId);
        return StreamTransferData.parse(taskStep.getJsonfiedTransferData());
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
        return DEFAULT_QUEUES;
    }

    private String getQueueName(final String prefix, final String application, final String taskID) {
        int hashcode = taskID.hashCode();
        int queue = hashcode % DEFAULT_QUEUES;
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(application).append("_").append(queue);
        return sb.toString();
    }

    private String getQueueName(final String prefix, final String application, final int queue) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(application).append("_").append(queue);
        return sb.toString();
    }
}

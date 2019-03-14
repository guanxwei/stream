package org.stream.extension.persist;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
    private TaskStorage kafkaBasedTaskStorage;

    @Setter
    private TaskStorage taskStorage;

    @Setter
    private TaskStepStorage taskStepStorage;

    @Setter
    private RedisService redisService;

    private static final String RETRY_KEY = "stream_auto_scheduled_retry_set_";
    private static final String BACKUP_KEY = "stream_auto_scheduled_backup_set_";

    private String application;

    @Setter
    private boolean debug = false;

    private static final String HOST_NAME = RandomStringUtils.randomAlphabetic(32);

    // Lock expire time in milliseconds.
    private static final long LOCK_EXPIRE_TIME = 10 * 1000;

    private static final int DEFAULT_QUEUES = 4;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean persist(final Task task) {
        /**
         * Persist the task in reliable storage and send a message to Kafka cluster if configured. 
         */
        return taskStorage.update(task) && kafkaBasedTaskStorage.persist(task);
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

        try {
            if (type == 1) {
                Set<String> result = redisService.zrange(getQueueName(RETRY_KEY, application, queue), Double.valueOf("0"),
                        System.currentTimeMillis());
                return result;
            } else if (type == 2) {
                return redisService.lrange(getQueueName(BACKUP_KEY, application, queue), 0, 10);
            } else if (type == 3) {
                List<Task> tasks = taskStorage.queryStuckTasks();
                if (!CollectionUtils.isEmpty(tasks)) {
                    return tasks.parallelStream()
                            .map(Task::getTaskId)
                            .collect(Collectors.toList());
                }
            }
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
        boolean result = redisService.setnx(taskId + "_lock", HOST_NAME) == 1L;
        if (result) {
            // No body else grabs the lock, we are the winner of contest;
            // Mark as locked.
            markAsLocked(taskId);
            return true;
        } else if (isLegibleOwner(taskId)) {
            // Retry within the legible time window, return true directly and refresh the lock time.
            markAsLocked(taskId);
            return true;
        } else {
            // Someone else owns the lock, we should check if it has expired.
            String time = redisService.get(taskId + "_locktime");
            long realTime = Long.parseLong(time);
            long now = System.currentTimeMillis();
            /** 
             * If the lock was hold longer then 10 seconds, we would think that the previous owner has crashed.
             * In most cases a remote call should be done in at most 3 seconds, here set the lock as triple the
             * most used timeout * 3 + 1 seconds.
             */
            if (now - realTime >= LOCK_EXPIRE_TIME) {
                /**
                 * The task has been locked too long, release the lock so as other contenders have chances to retry the work-flow.
                 */
                try {
                    // In case we disrupt other new contender retry the work, sleep for a while.
                    Thread.sleep(new Random(10).nextInt());
                    if (time.equals(redisService.get(taskId + "_locktime"))) {
                        // If it is still the previous lock owner, release the lock.
                        redisService.del(taskId + "_lock");
                    }
                } catch (InterruptedException e) {
                    log.error("Errpr", e);
                }
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
        redisService.set(taskId + "_locktime", String.valueOf(System.currentTimeMillis()));
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
            redisService.set(taskId + "_lock", HOST_NAME);
            markAsLocked(taskId);

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
        String owner = redisService.get(taskId + "_lock");
        String lockTime = redisService.get(taskId + "_locktime");
        if (owner == null) {
            return true;
        }

        if (owner.equals(HOST_NAME) && Long.valueOf(lockTime) + LOCK_EXPIRE_TIME > System.currentTimeMillis()) {
            return true;
        }

        return false;
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
        redisService.lrem(getQueueName(BACKUP_KEY, application, task.getTaskId()), 0, task.getTaskId());
        redisService.zadd(getQueueName(RETRY_KEY, application, task.getTaskId()), task.getTaskId(), score);
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

package org.stream.extension.admin;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.stream.core.exception.StreamException;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.persist.TaskStepStorage;
import org.stream.extension.persist.TaskStorage;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract of stream {@link Task} administrator, providing some useful methods to check the task's status, intercept the
 * execution life cycle of specific tasks, etc.
 *
 * Typically, users will construct their back-end admin systems to manage their tasks, for example list all the step details
 * a task was posted through, restart a failed task in the near future.
 *
 * If this class is needed, please let the spring location scanner scan this package.
 * @author weiguanxiong.
 *
 */
@Slf4j
public final class TaskAdministrator {

    @Resource
    private TaskPersister taskPersister;

    @Resource
    private TaskStorage taskStorage;

    @Resource
    private TaskStepStorage taskStepStorage;

    /**
     * Get all the task steps for the specific task.
     * @param taskId Target task's id.
     * @return Task step list for the task.
     */
    public List<TaskStep> getSteps(final String taskId) {
        List<TaskStep> taskSteps = taskStepStorage.getByTaskId(taskId);

        if (CollectionUtils.isNotEmpty(taskSteps)) {
            taskSteps.parallelStream().sorted((a, b) -> {
                return a.getCreateTime() > b.getCreateTime() ? 1 : 0;
            });
        }

        return taskSteps;
    }

    /**
     * Re-run the failed task if possible.
     * @param taskId Target task's id
     * @throws StreamException
     * @return <code>true</code> if the task's status if failed and re-ran successfully, otherwise <code>false</code>
     */
    public boolean reRunTask(final String taskId) throws StreamException {
        Task task = taskStorage.query(taskId);
        if (task == null) {
            log.info("Trying to re run unexisted task [{}]", taskId);
            throw new StreamException("Task not existed");
        }

        if (!task.getStatus().contentEquals("CompletedWithFailure")) {
            log.info("Trying to re run uncompleted task [{}]", taskId);
            throw new StreamException("Task is not completed, it will be automatically re-run in the near future");
        }

        task.setStatus("Executing");
        task.setRetryTimes(0);
        task.setLastExcutionTime(System.currentTimeMillis());
        task.setNextExecutionTime(System.currentTimeMillis() + 100);
        taskStorage.update(task);
        return true;
    }
}

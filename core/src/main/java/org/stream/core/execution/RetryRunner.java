package org.stream.core.execution;

import org.apache.commons.lang3.StringUtils;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.Node;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.persist.TaskPersister;

import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;

/**
 * Runnable implementation to process pending on retry tasks.
 *
 * Updated 2017/09/30:
 * We decided to support two strategies to re-run the suspended work-flow instances:
 * 1、Re-run the suspended instances periodically() with up-limit times 12. That's what we do here.
 * 2、Re-run the suspended instances at fixed time points, currently we will re-run the suspended work-flow at most
 *     12 times, the time points are:5s,10s,30s,60s,5m,10m,30m,1h,3h,10h,20h,24h,this strategy depends on Redis's zadd/zrange
 *     function, each time when we mark the work-flow as suspended, we also set up the expire time as the score for the task,
 *     before re-run the work-flow we will check if the time fulfill our requirements.
 *
 * See also {@linkplain AutoScheduledEngine#init()}, to Enable strategy 2, users should <p> explicitly
 * invoke the {@linkplain AutoScheduledEngine#setPattern(String)} before executing, set the pattern as "PubSub".
 * As the {@linkplain AutoScheduledEngine#init()} is invoked at initiation period(and executed only once), so the user should keep in mind
 * that the pattern will serve for the whole life cycle of the {@linkplain AutoScheduledEngine} instance. Users <p>should not change the pattern
 * for any reason, if both two strategies are needed, people should instantiate two instacces of {@linkplain AutoScheduledEngine}, each one
 * serves for exactly one strategy.
 *
 * This back-end runner serve both the strategy one and two, using parameter {@code RetryRunner#pattern]} to differentiate them.
 *
 */
@Slf4j
public class RetryRunner implements Runnable {

    private static final ImmutableList<Integer> SCHEDULED = ImmutableList.<Integer>builder()
            .add(5)
            .add(10)
            .add(20)
            .add(30)
            .add(240)
            .add(300)
            .add(1200)
            .add(1800)
            .add(7200)
            .add(25200)
            .add(36000)
            .add(14400)
            .build();

    private static final ImmutableList<Integer> EQUAL = ImmutableList.<Integer>builder()
            .add(10)
            .add(10)
            .add(10)
            .add(10)
            .add(10)
            .add(10)
            .add(10)
            .add(10)
            .add(10)
            .add(10)
            .add(10)
            .add(10)
            .build();

    private static final int MAX_RETRY = 11;

    private String content;
    private GraphContext graphContext;
    private TaskPersister taskPersister;
    private String pattern;

    /**
     * Constructor.
     * @param content Jsonfied {@link Task} entity.
     * @param graphContext Graph context.
     * @param taskPersister {@linkplain TaskPersister} entity.
     * @param pattern Retry pattern.
     */
    public RetryRunner(final String content, final GraphContext graphContext, final TaskPersister taskPersister,
            final String pattern) {
        this.content = content;
        this.graphContext = graphContext;
        this.taskPersister = taskPersister;
    }

    @Override
    public void run() {
        Task task = Task.parse(content);
        if (!task.getStatus().equals("PendingOnRetry")) {
            return;
        }

        StreamTransferData data = (StreamTransferData) StreamTransferData.parse(task.getJsonfiedTransferData());
        Node node = TaskHelper.deduceNode(task, graphContext);
        if (node == null) {
            log.error("Unvalid node, data [{}]", content);
            return;
        }

        if (!taskPersister.tryLock(task.getTaskId())) {
            //Some one else is being processing the task, quit.
            return;
        }

        Resource primaryResource = preparePrimaryResource(task, data);
        TaskHelper.prepare(task.getGraphName(), primaryResource, graphContext);

        while (node != null && !WorkFlowContext.provide().isRebooting()) {
            /**
             * Before executing the activity, we'd check if the node contains asynchronous dependency nodes,
             * if yes, we should construct some asynchronous tasks then turn back to the normal procedure.
             */
            if (node.getAsyncDependencies() != null) {
                TaskHelper.setUpAsyncTasks(WorkFlowContext.provide(), node);
            }

            ActivityResult activityResult = TaskHelper.perform(node);

            if (activityResult.equals(ActivityResult.SUSPEND)) {
                suspend(task, node, data);
                return;
            }

            TaskHelper.updateTask(task, node, "Executing");
            taskPersister.setHub(task.getTaskId(), task.toString());
            node = TaskHelper.traverse(activityResult, node);
        }
        TaskHelper.complete(task, data, taskPersister);
    }

    private Resource preparePrimaryResource(final Task task, final StreamTransferData data) {
        String primaryResourceString = task.getJsonfiedPrimaryResource();
        if (primaryResourceString != null) {
            Resource resource = Resource.parse(primaryResourceString);
            resource.setValue(data);
            return resource;
        }
        return null;
    }

    private void suspend(final Task task, final Node node, final StreamTransferData data) {
        // Persist workflow status to persistent layer.
        task.setNodeName(node.getNodeName());
        task.setJsonfiedPrimaryResource(WorkFlowContext.getPrimary().toString());
        task.setJsonfiedTransferData(data.toString());
        doSuspend(task, node);
    }

    private void doSuspend(final Task task, final Node node) {
        if (task.getRetryTimes() == MAX_RETRY && task.getNodeName().equals(node.getNodeName())) {
            // Will not try any more.
            taskPersister.complete(task);
            taskPersister.persist(task);
        } else if (task.getNodeName().equals(node.getNodeName()) && task.getRetryTimes() < MAX_RETRY) {
            task.setRetryTimes(task.getRetryTimes() + 1);
            taskPersister.suspend(task, getTime(task.getRetryTimes() + 1, pattern));
        } else {
            // Recount.
            task.setRetryTimes(0);
            taskPersister.suspend(task, getTime(0, pattern));
        }
    }

    /**
     * Get time window depends on the current pattern and the times have tried.
     * @param time Times that have been tried.
     * @param pattern Retry pattern.
     * @return Next time window
     */
    public static int getTime(final int time, final String pattern) {
        if (StringUtils.equals(RetryPattern.SCHEDULED_TIME, pattern)) {
            return SCHEDULED.get(time);
        } else if (StringUtils.equals(RetryPattern.EQUAL_DIFFERENCE, pattern)) {
            return EQUAL.get(time);
        } else {
            throw new RuntimeException("Unsupported pattern");
        }
    }
}

package org.stream.core.execution;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.extension.io.HessianIOSerializer;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.StreamTransferDataStatus;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStatus;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.persist.TaskPersister;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class providing some useful methods to easy the way execution runner
 * and retry runner process the tasks.
 * @author weiguanxiong
 *
 */
@Slf4j
public final class TaskExecutionUtils {

    private TaskExecutionUtils() { }

    /**
     * Before executing the activity, we'd check if the node contains asynchronous dependency nodes,
     * if yes, we should construct some asynchronous tasks then turn back to the normal procedure.
     * @param node Next node to be executed.
     */
    public static void prepareAsyncTasks(final Node node) {
        if (node.getAsyncDependencies() != null) {
            TaskHelper.setUpAsyncTasks(WorkFlowContext.provide(), node);
        }
    }

    /**
     * Update the task information based on the execution result of current node and graph definition.
     * @param task Processing task.
     * @param node Current node.
     * @param taskPersister Task persister used to update the task information.
     * @param graph Graph used to define the procedure.
     * @param activityResult Current node's execution result.
     * @return Next node to be executed.
     */
    public static Node updateTaskAndTraverseNode(final Task task, final Node node, final TaskPersister taskPersister, final Graph graph,
            final ActivityResult activityResult) {
        StreamTransferData data = (StreamTransferData) WorkFlowContext.resolveResource(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE).getValue();
        TaskHelper.updateTask(task, node, TaskStatus.PROCESSING.code());
        TaskStep taskStep = constructStep(graph, node, activityResult, data, task);
        taskPersister.initiateOrUpdateTask(task, false, taskStep);
        return TaskHelper.traverse(activityResult, node);
    }

    /**
     * Construct task step information based on node execution result.
     * @param graph Procedure graph.
     * @param node Current node.
     * @param activityResult Current node' execution result.
     * @param data Stream transfer data allocated for the task.
     * @param task Target task.
     * @return Constructed step entity.
     */
    public static TaskStep constructStep(final Graph graph, final Node node, final ActivityResult activityResult,
            final StreamTransferData data, final Task task) {
        TaskStep taskStep = TaskStep.builder()
                .createTime(System.currentTimeMillis())
                .graphName(graph.getGraphName())
                .nodeName(node.getNodeName())
                .status(activityResult.equals(ActivityResult.SUCCESS) ? StreamTransferDataStatus.SUCCESS : StreamTransferDataStatus.FAIL)
                .streamTransferData(HessianIOSerializer.encode(data))
                .taskId(task.getTaskId())
                .build();
        return taskStep;
    }

    public static void suspend(final Task task, final Node node, final TaskPersister taskPersister, final Graph graph,
            final RetryPattern pattern, final GraphContext graphContext) {
        int interval = TaskHelper.suspend(task, node, taskPersister, pattern);
        TaskHelper.retryLocalIfPossible(interval, task.getTaskId(), graphContext, taskPersister, pattern);
        log.info("Task [{}] suspended for interval [{}] at node [{}]", task.getTaskId(),
                interval, Node.CURRENT.get().getNodeName());
        WorkFlowContext.reboot();
    }
}

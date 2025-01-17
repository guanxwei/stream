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

import java.io.Serializable;

import org.stream.core.exception.DuplicateTaskException;
import org.stream.core.exception.WorkFlowExecutionException;
import org.stream.core.runtime.Jackson;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceCatalog;
import org.stream.core.resource.ResourceTank;
import org.stream.extension.events.Event;
import org.stream.extension.events.EventCenter;
import org.stream.extension.events.EventsHelper;
import org.stream.extension.events.WorkflowInitiatedEvent;
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.StreamTransferDataStatus;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStatus;
import org.stream.extension.persist.TaskPersister;
import org.stream.extension.settings.Settings;
import org.stream.extension.utils.TaskIDGenerator;
import org.stream.extension.utils.UUIDTaskIDGenerator;
import org.stream.extension.utils.actionable.Tellme;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Auto scheduled work-flow engine. This engine is mainly designed to support auto retry cases in distributed environments.
 * In a distributed world, multiple services will be arranged to complete one task; sometimes auto retry is also needed
 * to process temporary unavailable cases. It's tricky for developers themselves to determine what to do based on the
 * predecessor. There are so many things ahead to be done like invoking
 * the next step after something is executed, retry the step if it failed in pre-set times.
 * <p>
 * To help eliminate the effort of solving such tricky problems,
 * a stream work-flow framework provides this light flow engine implementation.
 * With this engine,
 * developers just need to implement their business logic in stand-alone activities
 * and manage the procedure through defining human friendly graphs.
 * This engine will do everything else silently,
 * including executing the missions in order and auto retry failed submissions, .etc.
 * <p>
 * Please be aware that currently AutoScheduledEngine does not support sub-work-flow situations. If you want to run sub procedures within another
 * work-flow context, you'd probably use other tools.
 */
@Setter
@Slf4j
public class AutoScheduledEngine implements Engine {

    private int maxRetry = 10;

    private ResourceCatalog resourceCatalog;

    private TaskPersister taskPersister;

    private String application;

    private TaskExecutor taskExecutor;

    private TaskIDGenerator taskIDGenerator = new UUIDTaskIDGenerator();

    private EventCenter eventCenter;

    /**
     * Not supported in auto scheduled engine, please do not use it.
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final boolean autoRecord) {
        throw new WorkFlowExecutionException(Settings.PRIMARY_MISSING_ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank execute(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final boolean autoRecord) {
        var taskId = start(graphName, graphContext, primaryResource.getValue(), null);
        var taskResource = Resource.builder()
                .value(taskId)
                .resourceReference(Settings.TASK_REFERENCE)
                .build();
        var tank = new ResourceTank();
        tank.addResource(taskResource);
        return tank;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final boolean autoRecord) {
        return execute(graphContext, graphName, primaryResource, autoRecord);
    }

    /**
     * Not supported in auto scheduled engine, please do not use it.
     */
    @Override
    public ResourceTank executeOnce(final GraphContext graphContext, final String graphName, final boolean autoRecord) {
        throw new WorkFlowExecutionException(Settings.PRIMARY_MISSING_ERROR);
    }

    /**
     * Not supported in auto scheduled engine, please do not use it.
     */
    @Override
    public ResourceTank executeFrom(final GraphContext graphContext, final String graphName, final String startNode,
            final boolean autoRecord) {
        throw new WorkFlowExecutionException(Settings.PRIMARY_MISSING_ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeFrom(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final String startNode, final boolean autoRecord) {
        var taskId = start(graphName, graphContext, primaryResource.getValue(), startNode);
        var taskResource = Resource.builder()
                .value(taskId)
                .resourceReference(Settings.TASK_REFERENCE)
                .build();
        var tank = new ResourceTank();
        tank.addResource(taskResource);
        return tank;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTank executeOnceFrom(final GraphContext graphContext, final String graphName, final Resource primaryResource,
            final String startNode, final boolean autoRecord) {
        return executeFrom(graphContext, graphName, primaryResource, startNode, autoRecord);
    }

    /**
     * Not supported in auto scheduled engine, please do not use it.
     */
    @Override
    public ResourceTank executeOnceFrom(final GraphContext graphContext, final String graphName, final String startNode,
            final boolean autoRecord) {
        throw new WorkFlowExecutionException(Settings.PRIMARY_MISSING_ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reboot() throws InterruptedException {
        Thread.interrupted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitAndReboot() {
        Thread.interrupted();
    }

    @SuppressWarnings("unchecked")
    private String start(final String graphName, final GraphContext graphContext, final Object resource,
            final String startNode) {

        if (!(resource instanceof Serializable)) {
            throw new WorkFlowExecutionException("Primary resource should be serializable when you are using auto schedule engine");
        }

        var primaryResource = Resource.builder()
                .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                .value(resource)
                .build();

        var taskId = taskIDGenerator.generateTaskID(primaryResource);

        log.info("Task id [{}] assigned to the request", taskId);

        var graph = graphContext.getGraph(graphName);

        Tellme.tryIt(() -> {
                    StreamTransferData data = new StreamTransferData();
                    Task task = initiateTask(taskId, graphName, primaryResource, data, graphContext, startNode);
                    EventsHelper.fireEvent(eventCenter, Event.of(WorkflowInitiatedEvent.class, task.getTaskId(),
                            graph.getStartNode()), false);
                    log.info("New task [{}] initiated", task.getTaskId());
                    taskExecutor.submit(primaryResource, task, data, this);
                    log.info("Task [{}] submitted", taskId);
                })
                .incase(DuplicateTaskException.class)
                .thenFix(e -> {
                    log.error("Duplicated task");
                    throw new WorkFlowExecutionException(e);
                });

        return taskId;
    }

    private Task initiateTask(final String taskId, final String graphName, final Resource primaryResource,
            final StreamTransferData data, final GraphContext graphContext, final String startNode) throws Exception {
        var graph = graphContext.getGraph(graphName);
        if (graph == null) {
            throw new WorkFlowExecutionException("Graph not existes! Please double check！");
        }

        if (taskPersister.get(taskId) != null) {
            throw new DuplicateTaskException();
        }

        var firstNode = startNode == null ? graph.getStartNode() : graph.getNode(startNode);
        if (firstNode == null) {
            log.error("Can not find the target node [{}] from the graph [{}]", startNode, graph.getGraphName());
            throw new WorkFlowExecutionException(String.format("Start node [%s] node exists in graph [%s]",
                    startNode, graphName));
        }
        var task = Task.builder()
                .application(application)
                .graphName(graphName)
                .initiatedTime(System.currentTimeMillis())
                .jsonfiedPrimaryResource(Jackson.json(primaryResource.getValue()))
                .lastExecutionTime(System.currentTimeMillis())
                .nextExecutionTime(System.currentTimeMillis() + 1000)
                .nodeName(firstNode.getNodeName())
                .retryTimes(0)
                .status(TaskStatus.INITIATED.code())
                .taskId(taskId)
                .build();
        data.add("primaryClass", primaryResource.getValue().getClass().getName());
        var taskStep = TaskExecutionUtils.constructStep(graph, firstNode,
                StreamTransferDataStatus.SUCCESS, data, task);
        taskPersister.initiateOrUpdateTask(task, true, taskStep);
        return task;
    }

}

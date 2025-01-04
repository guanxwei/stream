package org.stream.core.execution.test;

import static org.testng.Assert.assertFalse;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stream.core.component.ActivityRepository;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.execution.ExecutionRunner;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.TaskHelper;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.runtime.LocalGraphLoader;
import org.stream.core.resource.Resource;
import org.stream.extension.io.HessianIOSerializer;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStatus;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.persist.TaskPersister;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ExecutionRunnerTest {

    @InjectMocks
    private ExecutionRunner executionRunner;

    private Graph graph;

    private Resource primaryResource;

    private Task task;

    @Mock
    private TaskPersister taskPersister;

    @Mock
    private RetryPattern pattern;

    private GraphContext graphContext;

    @org.testng.annotations.BeforeMethod
    public void BeforeMethod() throws Exception {
        MockitoAnnotations.initMocks(this);
        List<String> paths = new LinkedList<>();
        paths.add("AutoScheduleNormal.graph");
        paths.add("AutoScheduleFull.graph");
        paths.add("AutoScheduleSuspend.graph");
        paths.add("AutoScheduleSuspend2.graph");
        paths.add("AutoScheduleSuspend3.graph");

        this.graphContext = new GraphContext();
        LocalGraphLoader graphLoader = new LocalGraphLoader();
        graphLoader.setGraphContext(graphContext);
        this.graphContext.setActivityRepository(new ActivityRepository());
        graphLoader.setGraphFilePaths(paths);
        graphLoader.init();
    }

    @Test
    public void testNormalCase() {
        primaryResource = Resource.builder()
                .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                .value("resource")
                .build();
        graph = TaskHelper.prepare("autoSchedule1", primaryResource, graphContext);

        StreamTransferData streamTransferData = new StreamTransferData();
        Resource dataResource = Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSFER_DATA_REFERENCE)
                .value(streamTransferData)
                .build();
        WorkFlowContext.attachResource(dataResource);
        task = Task.builder()
                .application("testApplication")
                .graphName("autoSchedule1")
                .id(0)
                .jsonfiedPrimaryResource(primaryResource.toString())
                .lastExecutionTime(System.currentTimeMillis())
                .retryTimes(0)
                .status(TaskStatus.INITIATED.code())
                .taskId(UUID.randomUUID().toString())
                .build();
        executionRunner = new ExecutionRunner(pattern, graphContext, primaryResource, task, taskPersister, dataResource, null);

        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);
        executionRunner.run();

        ArgumentCaptor<Task> captor2 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Boolean> captor3 = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<TaskStep> captor4 = ArgumentCaptor.forClass(TaskStep.class);

        Mockito.verify(taskPersister).initiateOrUpdateTask(captor2.capture(), captor3.capture(),
                captor4.capture());

        assertFalse(captor3.getValue());

        Task content = captor2.getValue();
        Assert.assertEquals(content.getNodeName(), "node4");
        Assert.assertEquals(content.getStatus(), TaskStatus.COMPLETED.code());

        Node node = graph.getNode("node4");
        Mockito.verify(taskPersister).complete(task, node);
        Mockito.verify(taskPersister).persist(task);

        Assert.assertEquals(task.getStatus(), TaskStatus.COMPLETED.code());

        assertFalse(WorkFlowContext.isThereWorkingWorkFlow());
    }

    @Test(dependsOnMethods = "testNormalCase")
    public void testSuspendCase() {
        primaryResource = Resource.builder()
                .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                .value("resource")
                .build();
        graph = TaskHelper.prepare("autoSchedule2", primaryResource, graphContext);

        StreamTransferData streamTransferData = new StreamTransferData();
        Resource dataResource = Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSFER_DATA_REFERENCE)
                .value(streamTransferData)
                .build();
        WorkFlowContext.attachResource(dataResource);
        task = Task.builder()
                .application("testApplication")
                .graphName("autoSchedule2")
                .id(0)
                .jsonfiedPrimaryResource(primaryResource.toString())
                .lastExecutionTime(System.currentTimeMillis())
                .retryTimes(0)
                .status(TaskStatus.INITIATED.code())
                .taskId(UUID.randomUUID().toString())
                .build();
        executionRunner = new ExecutionRunner(pattern, graphContext, primaryResource, task, taskPersister, dataResource, null);
        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);

        Mockito.when(pattern.getTimeInterval(0)).thenReturn(10);
        executionRunner.run();

        ArgumentCaptor<Task> captor1 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Double> captor2 = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<TaskStep> captor4 = ArgumentCaptor.forClass(TaskStep.class);
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);

        Mockito.verify(taskPersister).suspend(captor1.capture(), captor2.capture(), captor4.capture(), nodeCaptor.capture());

        Assert.assertEquals(captor2.getValue().intValue(), 10);
        Task captured = captor1.getValue();

        Assert.assertEquals(captured.getNodeName(), "node5");
        Assert.assertEquals(captured.getJsonfiedPrimaryResource(), task.getJsonfiedPrimaryResource());
        Assert.assertEquals(captured.getStatus(), TaskStatus.PENDING.code());
        assertFalse(WorkFlowContext.isThereWorkingWorkFlow());

    }

    @Test(dependsOnMethods = "testSuspendCase")
    public void testSuspendCase2() {
        primaryResource = Resource.builder()
                .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                .value("resource")
                .build();
        graph = TaskHelper.prepare("autoSchedule3", primaryResource, graphContext);

        StreamTransferData streamTransferData = new StreamTransferData();
        Resource dataResource = Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSFER_DATA_REFERENCE)
                .value(streamTransferData)
                .build();
        WorkFlowContext.attachResource(dataResource);
        task = Task.builder()
                .application("testApplication")
                .graphName("autoSchedule3")
                .id(0)
                .jsonfiedPrimaryResource(primaryResource.toString())
                .lastExecutionTime(System.currentTimeMillis())
                .retryTimes(0)
                .status(TaskStatus.INITIATED.code())
                .taskId(UUID.randomUUID().toString())
                .build();
        executionRunner = new ExecutionRunner(pattern, graphContext, primaryResource, task, taskPersister, dataResource, null);
        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);

        Mockito.when(pattern.getTimeInterval(0)).thenReturn(10);
        executionRunner.run();

        ArgumentCaptor<Task> captor1 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Double> captor2 = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<TaskStep> captor4 = ArgumentCaptor.forClass(TaskStep.class);
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);

        Mockito.verify(taskPersister).suspend(captor1.capture(), captor2.capture(), captor4.capture(), nodeCaptor.capture());

        Assert.assertEquals(captor2.getValue().intValue(), 10);
        Task captured = captor1.getValue();

        Assert.assertEquals(captured.getNodeName(), "node5");
        Assert.assertEquals(captured.getJsonfiedPrimaryResource(), task.getJsonfiedPrimaryResource());
        Assert.assertEquals(captor4.getValue().getStreamTransferData(), HessianIOSerializer.encode(streamTransferData));

        Assert.assertEquals(captured.getStatus(), TaskStatus.PENDING.code());
        assertFalse(WorkFlowContext.isThereWorkingWorkFlow());

    }
}

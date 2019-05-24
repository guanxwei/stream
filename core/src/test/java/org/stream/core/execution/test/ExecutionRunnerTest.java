package org.stream.core.execution.test;

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
import org.stream.core.execution.ExecutionRunner;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.TaskHelper;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.helper.LocalGraphLoader;
import org.stream.core.resource.Resource;
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

    private LocalGraphLoader graphLoader;
    private GraphContext graphContext;
    private List<String> paths;

    @org.testng.annotations.BeforeMethod
    public void BeforeMethod() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.paths = new LinkedList<String>();
        paths.add("AutoScheduleNormal.graph");
        paths.add("AutoScheduleFull.graph");
        paths.add("AutoScheduleSuspend.graph");
        paths.add("AutoScheduleSuspend2.graph");

        this.graphContext = new GraphContext();
        this.graphLoader = new LocalGraphLoader();
        graphLoader.setGraphContext(graphContext);
        this.graphContext.setActivityRepository(new ActivityRepository());
        graphLoader.setGraphFilePaths(paths);
        this.graphLoader.init();
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
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE)
                .value(streamTransferData)
                .build();
        WorkFlowContext.attachResource(dataResource);
        task = Task.builder()
                .application("testApplication")
                .graphName("autoSchedule1")
                .id(0)
                .jsonfiedPrimaryResource(primaryResource.toString())
                .lastExcutionTime(System.currentTimeMillis())
                .retryTimes(0)
                .status(TaskStatus.INITIATED.code())
                .taskId(UUID.randomUUID().toString())
                .build();
        executionRunner = new ExecutionRunner(graph, pattern, graphContext, primaryResource, task, taskPersister, dataResource);

        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);
        executionRunner.run();

        ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Task> captor2 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Boolean> captor3 = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<TaskStep> captor4 = ArgumentCaptor.forClass(TaskStep.class);

        Mockito.verify(taskPersister).setHub(captor1.capture(), captor2.capture(), captor3.capture(),
                captor4.capture());

        Assert.assertEquals(captor3.getValue().booleanValue(), false);
        Assert.assertEquals(captor1.getValue(), task.getTaskId());

        Task content = captor2.getValue();
        Assert.assertEquals(content.getNodeName(), "node4");
        Assert.assertEquals(content.getStatus(), TaskStatus.COMPLETED.code());

        Mockito.verify(taskPersister).complete(task);
        Mockito.verify(taskPersister).persist(task);

        Assert.assertEquals(task.getStatus(), TaskStatus.COMPLETED.code());
        WorkFlowContext.reboot();
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
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE)
                .value(streamTransferData)
                .build();
        WorkFlowContext.attachResource(dataResource);
        task = Task.builder()
                .application("testApplication")
                .graphName("autoSchedule2")
                .id(0)
                .jsonfiedPrimaryResource(primaryResource.toString())
                .lastExcutionTime(System.currentTimeMillis())
                .retryTimes(0)
                .status(TaskStatus.INITIATED.code())
                .taskId(UUID.randomUUID().toString())
                .build();
        executionRunner = new ExecutionRunner(graph, pattern, graphContext, primaryResource, task, taskPersister, dataResource);
        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);

        Mockito.when(pattern.getTimeInterval(0)).thenReturn(10);
        executionRunner.run();

        ArgumentCaptor<Task> captor1 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Double> captor2 = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<TaskStep> captor4 = ArgumentCaptor.forClass(TaskStep.class);

        Mockito.verify(taskPersister).suspend(captor1.capture(), captor2.capture(), captor4.capture());

        Assert.assertEquals(captor2.getValue().intValue(), 10);
        Task captured = captor1.getValue();

        Assert.assertEquals(captured.getNodeName(), "node5");
        Assert.assertEquals(captured.getJsonfiedPrimaryResource(), task.getJsonfiedPrimaryResource());
        Assert.assertEquals(captured.getStatus(), TaskStatus.PENDING.code());
        WorkFlowContext.reboot();
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
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE)
                .value(streamTransferData)
                .build();
        WorkFlowContext.attachResource(dataResource);
        task = Task.builder()
                .application("testApplication")
                .graphName("autoSchedule3")
                .id(0)
                .jsonfiedPrimaryResource(primaryResource.toString())
                .lastExcutionTime(System.currentTimeMillis())
                .retryTimes(0)
                .status(TaskStatus.INITIATED.code())
                .taskId(UUID.randomUUID().toString())
                .build();
        executionRunner = new ExecutionRunner(graph, pattern, graphContext, primaryResource, task, taskPersister, dataResource);
        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);

        Mockito.when(pattern.getTimeInterval(0)).thenReturn(10);
        executionRunner.run();

        ArgumentCaptor<Task> captor1 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Double> captor2 = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<TaskStep> captor4 = ArgumentCaptor.forClass(TaskStep.class);

        Mockito.verify(taskPersister).suspend(captor1.capture(), captor2.capture(), captor4.capture());

        Assert.assertEquals(captor2.getValue().intValue(), 10);
        Task captured = captor1.getValue();

        Assert.assertEquals(captured.getNodeName(), "node5");
        Assert.assertEquals(captured.getJsonfiedPrimaryResource(), task.getJsonfiedPrimaryResource());
        Assert.assertEquals(captor4.getValue().getJsonfiedTransferData(), streamTransferData.toString());
        Assert.assertEquals(captured.getStatus(), TaskStatus.PENDING.code());
        WorkFlowContext.reboot();
    }
}

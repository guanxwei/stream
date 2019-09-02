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
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.RetryRunner;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.helper.Jackson;
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

public class RetryRunnerTest {

    @InjectMocks
    private RetryRunner retryRunner;

    private String content;

    @Mock
    private TaskPersister taskPersister;

    @Mock
    private RetryPattern pattern;

    private LocalGraphLoader graphLoader;
    private GraphContext graphContext;
    private List<String> paths;

    private Resource primaryResource;

    private StreamTransferData data = new StreamTransferData();

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
        primaryResource = Resource.builder()
                .resourceReference("Auto::Scheduled::Workflow::PrimaryResource::Reference")
                .value("resource")
                .build();
        Mockito.when(taskPersister.retrieveData(Mockito.anyString())).thenReturn(data);
        data.add("primaryClass", String.class.getName());
    }

    @Test
    public void testCompleteCase() {
        Task task = Task.builder()
                .graphName("autoSchedule1")
                .jsonfiedPrimaryResource(Jackson.json(primaryResource.getValue()))
                .lastExcutionTime(System.currentTimeMillis() - 5 * 1000)
                .nodeName("node4")
                .retryTimes(3)
                .status(TaskStatus.COMPLETED.code())
                .taskId(UUID.randomUUID().toString())
                .build();

       content = task.toString();
       retryRunner = new RetryRunner(content, graphContext, taskPersister, pattern);
       retryRunner.run();

       ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
       Mockito.verify(taskPersister).complete(captor.capture());
       assertFalse(WorkFlowContext.isThereWorkingWorkFlow());

    }

    @Test
    public void testLockNotAbtainedCase() {
        Task task = Task.builder()
                .graphName("autoSchedule1")
                .jsonfiedPrimaryResource(Jackson.json(primaryResource.getValue()))
                .lastExcutionTime(System.currentTimeMillis() - 5 * 1000)
                .nodeName("node4")
                .retryTimes(3)
                .status(TaskStatus.PENDING.code())
                .taskId(UUID.randomUUID().toString())
                .build();

        content = task.toString();

        retryRunner = new RetryRunner(content, graphContext, taskPersister, pattern);

        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(false);
        retryRunner.run();
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        Mockito.verify(taskPersister, Mockito.times(0)).complete(captor.capture());
        assertFalse(WorkFlowContext.isThereWorkingWorkFlow());

    }

    @Test
    public void testNodeNotfoundCase() {
        Task task = Task.builder()
                .graphName("autoSchedule1")
                .jsonfiedPrimaryResource(Jackson.json(primaryResource.getValue()))
                .lastExcutionTime(System.currentTimeMillis() - 5 * 1000)
                .nodeName("node100")
                .retryTimes(3)
                .status(TaskStatus.PENDING.code())
                .taskId(UUID.randomUUID().toString())
                .build();

        content = task.toString();
        retryRunner = new RetryRunner(content, graphContext, taskPersister, pattern);
        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);

        retryRunner.run();

       ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
       Mockito.verify(taskPersister).complete(captor.capture());
       assertFalse(WorkFlowContext.isThereWorkingWorkFlow());

    }

    @Test
    public void testNodeNormalCase() {
        Task task = Task.builder()
                .graphName("autoSchedule1")
                .jsonfiedPrimaryResource(Jackson.json(primaryResource.getValue()))
                .lastExcutionTime(System.currentTimeMillis() - 5 * 1000)
                .nodeName("node4")
                .retryTimes(3)
                .status(TaskStatus.PENDING.code())
                .taskId(UUID.randomUUID().toString())
                .build();

        content = task.toString();
        retryRunner = new RetryRunner(content, graphContext, taskPersister, pattern);
        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);

        retryRunner.run();
        ArgumentCaptor<Task> captor2 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Boolean> captor3 = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Task> captor4 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Task> captor5 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<TaskStep> captor6 = ArgumentCaptor.forClass(TaskStep.class);

        Mockito.verify(taskPersister).initiateOrUpdateTask(captor2.capture(), captor3.capture(),
                captor6.capture());

        Assert.assertEquals(captor3.getValue().booleanValue(), false);
        //Assert.assertEquals(captor1.getValue(), task.getTaskId());

        Task content = captor2.getValue();
        Assert.assertEquals(content.getNodeName(), "node4");
        Assert.assertEquals(content.getStatus(), TaskStatus.COMPLETED.code());

        Mockito.verify(taskPersister).complete(captor4.capture());
        Mockito.verify(taskPersister).persist(captor5.capture());

        Assert.assertEquals(captor4.getValue(), captor5.getValue());
        Assert.assertEquals(captor4.getValue().getStatus(), TaskStatus.COMPLETED.code());
        assertFalse(WorkFlowContext.isThereWorkingWorkFlow());
    }

    @Test
    public void testSuspendCase() {
        Task task = Task.builder()
                .graphName("autoSchedule2")
                .jsonfiedPrimaryResource(Jackson.json(primaryResource.getValue()))
                .lastExcutionTime(System.currentTimeMillis() - 5 * 1000)
                .nodeName("node4")
                .retryTimes(3)
                .status(TaskStatus.PENDING.code())
                .taskId(UUID.randomUUID().toString())
                .build();

        content = task.toString();
        retryRunner = new RetryRunner(content, graphContext, taskPersister, pattern);
        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);
        Mockito.when(pattern.getTimeInterval(0)).thenReturn(10);

        retryRunner.run();

        ArgumentCaptor<Task> captor1 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Double> captor2 = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<TaskStep> captor6 = ArgumentCaptor.forClass(TaskStep.class);

        Mockito.verify(taskPersister).suspend(captor1.capture(), captor2.capture(), captor6.capture());

        Assert.assertEquals(captor2.getValue().intValue(), 10l);
        Task captured = captor1.getValue();

        Assert.assertEquals(captured.getNodeName(), "node5");
        Assert.assertEquals(captured.getJsonfiedPrimaryResource(), task.getJsonfiedPrimaryResource());
        Assert.assertEquals(captured.getStatus(), TaskStatus.PENDING.code());
        assertFalse(WorkFlowContext.isThereWorkingWorkFlow());
    }

    @Test
    public void testSuspendExhaustedCase() {
        taskPersister = Mockito.mock(TaskPersister.class);
        Task task = Task.builder()
                .graphName("autoSchedule2")
                .jsonfiedPrimaryResource(Jackson.json(primaryResource.getValue()))
                .lastExcutionTime(System.currentTimeMillis() - 5 * 1000)
                .nodeName("node5")
                .retryTimes(24)
                .status(TaskStatus.PENDING.code())
                .taskId(UUID.randomUUID().toString())
                .build();

        content = task.toString();
        retryRunner = new RetryRunner(content, graphContext, taskPersister, pattern);
        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);
        Mockito.when(pattern.getTimeInterval(1)).thenReturn(10);
        Mockito.when(taskPersister.retrieveData(Mockito.anyString())).thenReturn(data);

        retryRunner.run();

        ArgumentCaptor<Task> captor1 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Task> captor3 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Task> captor4 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<TaskStep> captor6 = ArgumentCaptor.forClass(TaskStep.class);

        ArgumentCaptor<Double> captor2 = ArgumentCaptor.forClass(Double.class);

        Mockito.verify(taskPersister, Mockito.times(0)).suspend(captor1.capture(), captor2.capture(),
                captor6.capture());

        Mockito.verify(taskPersister).complete(captor3.capture());
        Mockito.verify(taskPersister).persist(captor4.capture());

        Assert.assertEquals(captor3.getValue(), captor4.getValue());
        Assert.assertEquals(captor3.getValue().getStatus(), TaskStatus.FAILED.code());
        assertFalse(WorkFlowContext.isThereWorkingWorkFlow());
    }

    @Test
    public void testSuspendAgainCase() {
        taskPersister = Mockito.mock(TaskPersister.class);
        Task task = Task.builder()
                .graphName("autoSchedule2")
                .jsonfiedPrimaryResource(Jackson.json(primaryResource.getValue()))
                .lastExcutionTime(System.currentTimeMillis() - 5 * 1000)
                .nodeName("node5")
                .retryTimes(5)
                .status(TaskStatus.PENDING.code())
                .taskId(UUID.randomUUID().toString())
                .build();

        content = task.toString();
        retryRunner = new RetryRunner(content, graphContext, taskPersister, pattern);
        Mockito.when(taskPersister.tryLock(task.getTaskId())).thenReturn(true);
        Mockito.when(pattern.getTimeInterval(6)).thenReturn(10);
        Mockito.when(taskPersister.retrieveData(Mockito.anyString())).thenReturn(data);

        retryRunner.run();

        ArgumentCaptor<Task> captor1 = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Double> captor2 = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<TaskStep> captor6 = ArgumentCaptor.forClass(TaskStep.class);

        Mockito.verify(taskPersister).suspend(captor1.capture(), captor2.capture(),
                captor6.capture());

        Assert.assertEquals(captor2.getValue().intValue(), 10);
        Task captured = captor1.getValue();

        Assert.assertEquals(captured.getNodeName(), "node5");
        Assert.assertEquals(captured.getJsonfiedPrimaryResource(), task.getJsonfiedPrimaryResource());
        Assert.assertEquals(captured.getStatus(), TaskStatus.PENDING.code());
        assertFalse(WorkFlowContext.isThereWorkingWorkFlow());
    }
}

package org.stream.core.execution.test;

import java.util.LinkedList;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stream.core.component.ActivityRepository;
import org.stream.core.execution.AutoScheduledEngine;
import org.stream.core.execution.GraphContext;
import org.stream.core.helper.GraphLoader;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceCatalog;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.ResourceType;
import org.stream.extension.executors.MockExecutorService;
import org.stream.extension.executors.ThreadPoolTaskExecutor;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.pattern.RetryPattern;
import org.stream.extension.pattern.defaults.EqualTimeIntervalPattern;
import org.stream.extension.persist.TaskPersister;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AutoScheduledEngineTest {

    @InjectMocks
    private AutoScheduledEngine autoScheduledEngine;

    private GraphContext graphContext;

    private List<String> paths;

    private GraphLoader graphLoader;

    @Mock
    private ResourceCatalog resourceCatalog;

    @Mock
    private TaskPersister taskPersister;

    private RetryPattern retryPattern;

    private String application = "testApplication";

    @BeforeMethod
    public void BeforeClass() throws Exception {
        MockitoAnnotations.initMocks(this);
        autoScheduledEngine.setApplication(application);
        retryPattern = new EqualTimeIntervalPattern();
        this.paths = new LinkedList<String>();
        paths.add("AutoScheduleNormal.graph");
        paths.add("AutoScheduleFull.graph");
        paths.add("AutoScheduleSuspend.graph");
        paths.add("AutoScheduleSuspend2.graph");

        this.graphContext = new GraphContext();
        autoScheduledEngine.setGraphContext(graphContext);
        this.graphLoader = new GraphLoader();
        graphLoader.setGraphContext(graphContext);
        this.graphContext.setActivityRepository(new ActivityRepository());
        graphLoader.setGraphFilePaths(paths);
        this.graphLoader.init();
        autoScheduledEngine.setTaskExecutor(new ThreadPoolTaskExecutor(new MockExecutorService(), taskPersister, retryPattern, graphContext));
    }

    @Test
    public void testNormal() throws Exception {
        ResourceTank tank = autoScheduledEngine.execute(graphContext, "autoSchedule1", false, ResourceType.OBJECT);
        Resource resource = tank.resolve(AutoScheduledEngine.TASK_REFERENCE);

        Assert.assertNotNull(resource);
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);

        Mockito.verify(taskPersister).complete(captor.capture());

        Assert.assertEquals(captor.getValue().getStatus(), "Completed");
        Assert.assertEquals(captor.getValue().getNodeName(), "node4");
        Assert.assertEquals(captor.getValue().getGraphName(), "autoSchedule1");

        Assert.assertEquals(resource.getValue(), captor.getValue().getTaskId());
    }

    @Test
    public void testSuspend() throws Exception  {
        Mockito.when(taskPersister.tryLock(Mockito.anyString())).thenReturn(true);
        ResourceTank tank = autoScheduledEngine.execute(graphContext, "autoSchedule2", false, ResourceType.OBJECT);

        Resource resource = tank.resolve(AutoScheduledEngine.TASK_REFERENCE);

        Assert.assertNotNull(resource);
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        ArgumentCaptor<Integer> captor2 = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<TaskStep> captor3 = ArgumentCaptor.forClass(TaskStep.class);
        Mockito.verify(taskPersister).suspend(captor.capture(), captor2.capture(), captor3.capture());

        Assert.assertEquals(captor.getValue().getStatus(), "PendingOnRetry");
        Assert.assertEquals(captor.getValue().getNodeName(), "node5");
        Assert.assertEquals(captor.getValue().getGraphName(), "autoSchedule2");

        Assert.assertEquals(resource.getValue(), captor.getValue().getTaskId());
    }
}
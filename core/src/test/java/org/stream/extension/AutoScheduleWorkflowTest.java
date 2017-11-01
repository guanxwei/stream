package org.stream.extension;

import java.util.LinkedList;
import java.util.List;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stream.core.component.ActivityRepository;
import org.stream.core.execution.AutoScheduledEngine;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.RetryPattern;
import org.stream.core.helper.GraphLoader;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.ResourceType;
import org.stream.extension.persist.RedisService;
import org.stream.extension.persist.TaskDao;
import org.stream.extension.persist.TaskPersisterRedisImpl;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AutoScheduleWorkflowTest {

    @Mock
    private TaskDao taskDao;

    private RedisService redisService;
    private AutoScheduledEngine engine;
    private GraphContext graphContext;
    private GraphLoader graphLoader;
    private ActivityRepository activityRepository;
    private List<String> paths;
    private TaskPersisterRedisImpl taskPersister;

    @BeforeMethod(groups = "ignore")
    public void BeforeMethod() {
        MockitoAnnotations.initMocks(this);
        this.redisService = new RedisService("m1;m2",
                "10.165.124.49:26379;10.165.124.49:26380;10.165.124.49:26382", null, 5000, 10);
        this.activityRepository = new ActivityRepository();
        this.graphContext = new GraphContext();
        this.graphLoader = new GraphLoader();
        graphContext.setActivityRepository(activityRepository);
        graphLoader.setGraphContext(graphContext);
        paths = new LinkedList<>();
        graphLoader.setGraphFilePaths(paths);
        this.engine = new AutoScheduledEngine();
        this.taskPersister = new TaskPersisterRedisImpl();
        taskPersister.setTaskDao(taskDao);
        taskPersister.setRedisService(redisService);
        taskPersister.setApplication("TestStream");
        engine.setTaskPersister(taskPersister);
        engine.setGraphContext(graphContext);
        Mockito.when(taskDao.persist(Mockito.any())).thenReturn(true);
    }

    @Test(groups = "ignore")
    public void testAutoRetry() throws Exception {
        String path = "AuthRecover.graph";
        paths.add(path);
        graphLoader.init();
        engine.init();
        ResourceTank tank = engine.execute(graphContext, "autoRetain", false, ResourceType.OBJECT);
        Resource taskId = tank.resolve(AutoScheduledEngine.TASK_REFERENCE);
        String id = (String) taskId.getValue();
        Assert.assertNotNull(id);
        // Wait for the back-end job to complete execution.
        Thread.sleep(3000);
    }

    @Test(groups = "ignore")
    public void testAutoRetryWithSchedulePattern() throws Exception {
        String path = "AuthRecover.graph";
        paths.add(path);
        graphLoader.init();
        AutoScheduledEngine autoScheduledEngine = new AutoScheduledEngine();
        autoScheduledEngine.init();
        autoScheduledEngine.setPattern(RetryPattern.SCHEDULED_TIME);
        autoScheduledEngine.setTaskPersister(taskPersister);
        autoScheduledEngine.setGraphContext(graphContext);
        ResourceTank tank = autoScheduledEngine.execute(graphContext, "autoRetain", false, ResourceType.OBJECT);
        Resource taskId = tank.resolve(AutoScheduledEngine.TASK_REFERENCE);
        String id = (String) taskId.getValue();
        Assert.assertNotNull(id);
        // Wait for the back-end job to complete execution.
        Thread.sleep(9000);

    }
}

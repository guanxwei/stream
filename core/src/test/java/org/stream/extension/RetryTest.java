package org.stream.extension;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stream.core.component.ActivityRepository;
import org.stream.core.execution.AutoScheduledEngine;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.RetryPattern;
import org.stream.core.execution.RetryRunner;
import org.stream.core.helper.GraphLoader;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;
import org.stream.extension.persist.RedisService;
import org.stream.extension.persist.TaskDao;
import org.stream.extension.persist.TaskPersisterRedisImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RetryTest {

    @Mock
    private TaskDao taskDao;
    private RedisService redisService;
    private AutoScheduledEngine engine;
    private GraphContext graphContext;
    private GraphLoader graphLoader;
    private ActivityRepository activityRepository;
    private List<String> paths;

    @Test(groups = "ignore")
    public void test() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.redisService = new RedisService("m1;m2",
                "10.165.124.49:26379;10.165.124.49:26380;10.165.124.49:26382", null, 5000, 10);
        this.activityRepository = new ActivityRepository();
        this.graphContext = new GraphContext();
        this.graphLoader = new GraphLoader();
        graphContext.setActivityRepository(activityRepository);
        graphLoader.setGraphContext(graphContext);
        paths = new LinkedList<>();
        paths.add("AuthRecover.graph");
        graphLoader.setGraphFilePaths(paths);
        this.engine = new AutoScheduledEngine();
        TaskPersisterRedisImpl taskPersister = new TaskPersisterRedisImpl();
        taskPersister.setTaskDao(taskDao);
        taskPersister.setRedisService(redisService);
        taskPersister.setDebug(true);
        taskPersister.setApplication("TestStream");
        engine.setTaskPersister(taskPersister);
        engine.setGraphContext(graphContext);
        Mockito.when(taskDao.persist(Mockito.any())).thenReturn(true);
        graphLoader.init();
        StreamTransferData data = new StreamTransferData();
        Resource primaryResource = Resource.builder()
                .value(data)
                .resourceReference("Random")
                .build();
        data.set("Count", "Hello");
        Task task = new Task();
        task.setGraphName("autoRetain");
        task.setJsonfiedPrimaryResource(primaryResource.toString());
        task.setJsonfiedTransferData(data.toString());
        task.setLastExcutionTime(System.currentTimeMillis());
        task.setNodeName("node1");
        task.setRetryTimes(0);
        task.setStatus("PendingOnRetry");
        task.setTaskId(UUID.randomUUID().toString());
        RetryRunner runner = new RetryRunner(task.toString(), graphContext, taskPersister, RetryPattern.EQUAL_DIFFERENCE);
        runner.run();
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        Mockito.verify(taskDao).persist(taskCaptor.capture());
        Task captoredTask = taskCaptor.getValue();
        Assert.assertEquals(captoredTask.getStatus(), "Completed");
    }
}

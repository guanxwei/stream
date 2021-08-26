package org.stream.extension;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.test.MockRedisClient;
import org.stream.extension.clients.RedisClient;
import org.stream.extension.meta.Task;
import org.stream.extension.meta.TaskStep;
import org.stream.extension.persist.DelayQueue;
import org.stream.extension.persist.FifoQueue;
import org.stream.extension.persist.QueueHelper;
import org.stream.extension.persist.TaskPersisterImpl;
import org.stream.extension.persist.TaskStepStorage;
import org.stream.extension.persist.TaskStorage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TaskPersisterImplTest {

    @InjectMocks
    public TaskPersisterImpl taskPersisterImpl;

    @Mock
    private TaskStorage messageQueueBasedTaskStorage;

    @Mock
    private TaskStorage taskStorage;

    @Mock
    private TaskStepStorage taskStepStorage;

    private RedisClient redisClient;

    @Mock
    private DelayQueue delayQueue;

    @Mock
    private FifoQueue fifoQueue;

    private String application = "test";

    @BeforeMethod
    public void BeforeMethod() {
        MockitoAnnotations.initMocks(this);
        taskPersisterImpl.setApplication(application);
        redisClient = new MockRedisClient();
        taskPersisterImpl.setRedisClient(redisClient);
    }

    @Test
    public void testTryLock() {
        String taskID = RandomStringUtils.randomAlphabetic(12);
        assertTrue(taskPersisterImpl.tryLock(taskID));
        String queue = QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, taskID);
        String queue2 = QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, taskID);
        assertTrue(taskPersisterImpl.isProcessing(taskID));
        Mockito.verify(delayQueue).deleteItem(queue, taskID);
        Mockito.verify(fifoQueue).push(queue2, taskID);
    }

    @Test
    public void testTryLock2() throws Throwable {
        String taskID = RandomStringUtils.randomAlphabetic(12);
        Thread t = new Thread(() -> {
            assertTrue(taskPersisterImpl.tryLock(taskID));
            String queue = QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, taskID);
            String queue2 = QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, taskID);
            assertTrue(taskPersisterImpl.isProcessing(taskID));
            Mockito.verify(delayQueue).deleteItem(queue, taskID);
            Mockito.verify(fifoQueue).push(queue2, taskID);
        }) ;
        t.start();
        Thread.sleep(1000);

        assertFalse(taskPersisterImpl.tryLock(taskID));
        taskPersisterImpl.releaseLock(taskID);
        assertTrue(taskPersisterImpl.tryLock(taskID));

    }

    @Test
    public void testTryLock3() {
        String taskID = RandomStringUtils.randomAlphabetic(12);
        assertTrue(taskPersisterImpl.tryLock(taskID));
        String queue = QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, taskID);
        String queue2 = QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, taskID);
        assertTrue(taskPersisterImpl.isProcessing(taskID));
        Mockito.verify(delayQueue).deleteItem(queue, taskID);
        Mockito.verify(fifoQueue).push(queue2, taskID);
        taskPersisterImpl.releaseLock(taskID);

        assertTrue(taskPersisterImpl.tryLock(taskID));
    }

    @Test
    public void tetTrylockexpired() {
        RedisClient redisClient = Mockito.mock(RedisClient.class);
        taskPersisterImpl.setRedisClient(redisClient);
        Mockito.when(redisClient.setnxWithExpireTime(Mockito.anyString(), Mockito.anyString())).thenReturn(0l);
        String taskID = RandomStringUtils.randomAlphabetic(12);
        Mockito.when(redisClient.get(taskID + "_lock")).thenReturn("fdafdafaf_102102");

        assertFalse(taskPersisterImpl.tryLock(taskID));
    }

    @Test
    public void testPersist() {
        Task task = new Task();
        Mockito.when(taskStorage.update(task)).thenReturn(true);

        taskPersisterImpl.persist(task);
        Mockito.verify(taskStorage).update(task);
        Mockito.verify(messageQueueBasedTaskStorage).persist(task);
    }

    @Test
    public void testGet() {
        taskPersisterImpl.get("test");
        Mockito.verify(taskStorage).query("test");
    }

    @Test
    public void testReleaseLock() {
        RedisClient redisClient = Mockito.mock(RedisClient.class);
        taskPersisterImpl.setRedisClient(redisClient);
        taskPersisterImpl.releaseLock("test");
        Mockito.verify(redisClient).del("test_lock");
        assertFalse(taskPersisterImpl.isProcessing("test"));
    }

    @Test
    public void testInitiateOrUpdateTask() {
        Task task = new Task();
        task.setTaskId(RandomStringUtils.randomAlphabetic(10));
        TaskStep taskStep = new TaskStep();
        Mockito.when(taskStepStorage.insert(taskStep)).thenReturn(true);
        taskPersisterImpl.initiateOrUpdateTask(task, true, taskStep);
        Mockito.verify(taskStorage).persist(task);
        Mockito.verify(taskStepStorage).insert(taskStep);
    }

    @Test
    public void testInitiateOrUpdateTask2() {
        Task task = new Task();
        task.setTaskId(RandomStringUtils.randomAlphabetic(10));
        TaskStep taskStep = new TaskStep();
        Mockito.when(taskStepStorage.insert(taskStep)).thenReturn(true);
        taskPersisterImpl.initiateOrUpdateTask(task, false, taskStep);
        Mockito.verify(taskStorage).update(task);
        Mockito.verify(taskStepStorage).insert(taskStep);
    }

    @Test(expectedExceptions = WorkFlowExecutionExeception.class)
    public void testInitiateOrUpdateTask3() {
        RedisClient redisClient = Mockito.mock(RedisClient.class);
        taskPersisterImpl.setRedisClient(redisClient);
        Task task = new Task();
        task.setTaskId(RandomStringUtils.randomAlphabetic(10));
        TaskStep taskStep = new TaskStep();
        Mockito.when(redisClient.get(task.getTaskId() + "_lock")).thenReturn("fdfad");
        taskPersisterImpl.initiateOrUpdateTask(task, false, taskStep);
    }

    @Test
    public void testFastReentry() {
        RedisClient redisClient = Mockito.mock(RedisClient.class);
        taskPersisterImpl.setRedisClient(redisClient);
        Task task = new Task();
        task.setTaskId(RandomStringUtils.randomAlphabetic(10));
        Mockito.when(redisClient.setnxWithExpireTime(Mockito.anyString(), Mockito.anyString())).thenReturn(1l);
        taskPersisterImpl.tryLock(task.getTaskId());
        assertTrue(taskPersisterImpl.tryLock(task.getTaskId()));
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> captor2 = ArgumentCaptor.forClass(String.class);
        Mockito.verify(redisClient).setnxWithExpireTime(captor.capture(), captor2.capture());
    }

    @Test
    public void testOnehostTwoThread() throws Throwable {
        RedisClient redisClient = Mockito.mock(RedisClient.class);
        taskPersisterImpl.setRedisClient(redisClient);
        Task task = new Task();
        task.setTaskId(RandomStringUtils.randomAlphabetic(10));
        Mockito.when(redisClient.setnxWithExpireTime(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(1l).thenReturn(0l);
        new Thread(() -> {
            taskPersisterImpl.tryLock(task.getTaskId());
        }).start();

        Thread.sleep(300);
        assertFalse(taskPersisterImpl.tryLock(task.getTaskId()));
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> captor2 = ArgumentCaptor.forClass(String.class);
        Mockito.verify(redisClient, Mockito.times(1)).setnxWithExpireTime(captor.capture(), captor2.capture());
    }
}

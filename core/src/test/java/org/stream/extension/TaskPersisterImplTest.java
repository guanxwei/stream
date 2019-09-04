package org.stream.extension;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stream.core.exception.WorkFlowExecutionExeception;
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

    @Mock
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
    }

    @Test
    public void testTryLock() {
        Mockito.when(redisClient.setnx(Mockito.anyString(), Mockito.anyString())).thenReturn(1l);
        String taskID = RandomStringUtils.randomAlphabetic(12);
        assertTrue(taskPersisterImpl.tryLock(taskID));
        String queue = QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, taskID);
        String queue2 = QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, taskID);
        assertTrue(taskPersisterImpl.isProcessing(taskID));
        Mockito.verify(delayQueue).deleteItem(queue, taskID);
        Mockito.verify(fifoQueue).push(queue2, taskID);
    }

    @Test
    public void testTryLock2() {
        Mockito.when(redisClient.setnx(Mockito.anyString(), Mockito.anyString())).thenReturn(1l).thenReturn(0L);
        String taskID = RandomStringUtils.randomAlphabetic(12);
        assertTrue(taskPersisterImpl.tryLock(taskID));
        String queue = QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY, application, taskID);
        String queue2 = QueueHelper.getQueueNameFromTaskID(QueueHelper.BACKUP_KEY, application, taskID);
        assertTrue(taskPersisterImpl.isProcessing(taskID));
        Mockito.verify(delayQueue).deleteItem(queue, taskID);
        Mockito.verify(fifoQueue).push(queue2, taskID);

        assertFalse(taskPersisterImpl.tryLock(taskID));
    }

    @Test
    public void testTryLock3() {
        Mockito.when(redisClient.setnx(Mockito.anyString(), Mockito.anyString())).thenReturn(1l).thenReturn(0l);
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
        Mockito.when(redisClient.setnx(Mockito.anyString(), Mockito.anyString())).thenReturn(0l);
        String taskID = RandomStringUtils.randomAlphabetic(12);
        Mockito.when(redisClient.get(taskID + "_lock")).thenReturn("fdafdafaf_102102");

        assertFalse(taskPersisterImpl.tryLock(taskID));
        Mockito.verify(redisClient).del(taskID+ "_lock");
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
    public void testGetPendingList1() {
        taskPersisterImpl.getPendingList(1, 1);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> captor2 = ArgumentCaptor.forClass(Double.class);

        Mockito.verify(delayQueue).getItems(captor.capture(), captor2.capture());
        assertEquals(QueueHelper.getQueueNameFromIndex(QueueHelper.RETRY_KEY, application, 1), captor.getValue());
    }

    @Test
    public void testGetPendingList2() {
        taskPersisterImpl.getPendingList(2, 1);
        Mockito.verify(fifoQueue).pop(
                QueueHelper.getQueueNameFromIndex(QueueHelper.BACKUP_KEY, application, 1), 10);
    }

    @Test
    public void testGetPendingList3() {
        taskPersisterImpl.getPendingList(3, 1);
        Mockito.verify(taskStorage).queryStuckTasks();
    }

    @Test
    public void testReleaseLock() {
        taskPersisterImpl.releaseLock("test");
        Mockito.verify(redisClient).del("test_lock");
        assertFalse(taskPersisterImpl.isProcessing("test"));
    }

    @Test
    public void testInitiateOrUpdateTask() {
        Task task = new Task();
        TaskStep taskStep = new TaskStep();
        Mockito.when(taskStepStorage.insert(taskStep)).thenReturn(true);
        taskPersisterImpl.initiateOrUpdateTask(task, true, taskStep);
        Mockito.verify(taskStorage).persist(task);
        Mockito.verify(taskStepStorage).insert(taskStep);
        
    }

    @Test
    public void testInitiateOrUpdateTask2() {
        Task task = new Task();
        TaskStep taskStep = new TaskStep();
        Mockito.when(taskStepStorage.insert(taskStep)).thenReturn(true);

        taskPersisterImpl.initiateOrUpdateTask(task, false, taskStep);
        Mockito.verify(taskStorage).update(task);
        Mockito.verify(taskStepStorage).insert(taskStep);
        
    }

    @Test(expectedExceptions = WorkFlowExecutionExeception.class)
    public void testInitiateOrUpdateTask3() {
        Task task = new Task();
        task.setTaskId("dfafdafaf");
        TaskStep taskStep = new TaskStep();
        Mockito.when(redisClient.get("dfafdafaf_lock")).thenReturn("fdfad");
        taskPersisterImpl.initiateOrUpdateTask(task, false, taskStep);
    }
}

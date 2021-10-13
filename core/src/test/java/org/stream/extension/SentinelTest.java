package org.stream.extension;

import static org.testng.Assert.assertEquals;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stream.core.execution.Sentinel;
import org.stream.extension.executors.TaskExecutor;
import org.stream.extension.lock.Lock;
import org.stream.extension.persist.DelayQueue;
import org.stream.extension.persist.FifoQueue;
import org.stream.extension.persist.QueueHelper;
import org.stream.extension.persist.TaskPersisterImpl;
import org.stream.extension.persist.TaskStorage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class SentinelTest {
    @InjectMocks
    private Sentinel sentinel;

    @Mock
    private TaskExecutor taskExecutor;
    private TaskPersisterImpl taskPersister = new TaskPersisterImpl();
    @Mock
    private DelayQueue delayQueue;
    @Mock
    private FifoQueue fifoQueue;
    @Mock
    private TaskStorage taskStorage;
    @Mock
    private Lock lock;

    @BeforeMethod
    public void BeforeMethod() {
        MockitoAnnotations.initMocks(this);
        taskPersister.setApplication("application");
        taskPersister.setLock(lock);
        sentinel.setTaskPersister(taskPersister);
    }

    @Test
    public void testGetPendingList1() {
        sentinel.getPendingList(1, 1);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> captor2 = ArgumentCaptor.forClass(Double.class);

        Mockito.verify(delayQueue).getItems(captor.capture(), captor2.capture());
        assertEquals(QueueHelper.getQueueNameFromIndex(QueueHelper.RETRY_KEY, taskPersister.getApplication(), 1), captor.getValue());
    }

    @Test
    public void testGetPendingList2() {
        sentinel.getPendingList(2, 1);
        Mockito.verify(fifoQueue).pop(
                QueueHelper.getQueueNameFromIndex(QueueHelper.BACKUP_KEY, taskPersister.getApplication(), 1), 10);
    }

    @Test
    public void testGetPendingList3() {
        sentinel.getPendingList(3, 1);
        Mockito.verify(taskStorage).queryStuckTasks();
    }
}

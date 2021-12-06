package org.stream.extension;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stream.core.execution.Engine;
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
    @Mock
    private Engine engine;

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

    @Test
    public void testInit() {
        ScheduledExecutorService mock = Mockito.mock(ScheduledExecutorService.class);
        sentinel.setScheduledExecutorService(mock);
        sentinel.init();

        ArgumentCaptor<Runnable> captor1 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> captor2 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> captor3 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> captor4 = ArgumentCaptor.forClass(TimeUnit.class);

        Mockito.verify(mock, Mockito.times(17)).scheduleAtFixedRate(captor1.capture(), captor2.capture(), captor3.capture(), captor4.capture());
    }
}

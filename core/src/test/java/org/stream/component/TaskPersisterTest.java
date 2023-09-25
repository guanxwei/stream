package org.stream.component;

import static org.testng.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.stream.extension.persist.QueueHelper;
import org.stream.extension.persist.TaskPersisterImpl;
import org.testng.annotations.Test;

public class TaskPersisterTest {

    private TaskPersisterImpl taskPersisterImpl = new TaskPersisterImpl();

    @Test
    public void test() {
        taskPersisterImpl.setApplication("testApplication");
        Set<String> queues = new HashSet<String>();
        for (int i = 0; i < QueueHelper.DEFAULT_QUEUES; i++) {
            String queue = QueueHelper.RETRY_KEY + "testApplication" + "_" + i;
            queues.add(queue);
        }
        for (int i = 0; i < QueueHelper.DEFAULT_QUEUES; i++) {
            String queue = QueueHelper.getQueueNameFromIndex(QueueHelper.RETRY_KEY,
                    taskPersisterImpl.getApplication(), i);
            assertTrue(queues.contains(queue));
        }

        for (int i = 0; i < 10000; i++) {
            String taskId = RandomStringUtils.random(32);
            String queue = QueueHelper.getQueueNameFromTaskID(QueueHelper.RETRY_KEY,
                    taskPersisterImpl.getApplication(), taskId);
            assertTrue(queues.contains(queue));
        }
    }
}

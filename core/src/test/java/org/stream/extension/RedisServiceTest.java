package org.stream.extension;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.stream.extension.meta.Task;
import org.stream.extension.persist.RedisService;
import org.stream.extension.persist.TaskPersisterRedisImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import redis.clients.jedis.Tuple;

public class RedisServiceTest {

    private RedisService redisService;

    @org.testng.annotations.BeforeMethod(groups = "ignore")
    public void BeforeMethod() {
        this.redisService = new RedisService("m1;m2", "10.165.124.49:26379;10.165.124.49:26380;10.165.124.49:26382", null, 5000, 10);
    }

    @Test(groups = "ignore")
    public void testSet() {
        String key = "Payments_Stream_Workflow_Redis_Key_Test";
        String value = "helloworld";
        redisService.set(key, value);
        String retrived = redisService.get(key);
        Assert.assertEquals(value, retrived);
        redisService.deleteKey(key);
        Assert.assertNull(redisService.get(key));
    }

    @Test(groups = "ignore")
    public void testList() {
        String key = "Payments_Stream_Workflow_Redis_Key_List_Test";
        String value1 = "hello1";
        String value2 = "hello2";
        redisService.sadd(key, value1);
        redisService.sadd(key, value2);
        List<String> values = redisService.getList(key, 10);
        Assert.assertNotNull(values);
        Assert.assertFalse(values.isEmpty());
        Assert.assertEquals(values.size(), 2);
        redisService.sremove(key, value1, value2);
        values = redisService.getList(key, 10);
        Assert.assertTrue(values.isEmpty());
    }

    @Test(groups = "ignore")
    public void testDeleteSet() {
        List<String> keys = redisService.getList("stream_auto_scheduled_set_" + TaskPersisterRedisImpl.class.getSimpleName(), 1000);
        for (String key : keys) {
            redisService.del(key);
        }
        redisService.deleteKey("stream_auto_scheduled_set_" + TaskPersisterRedisImpl.class.getSimpleName());
    }

    @Test(groups = "ignore")
    public void testLock() {
        String taskId = UUID.randomUUID().toString();
        TaskPersisterRedisImpl redis = new TaskPersisterRedisImpl();
        redis.setRedisService(redisService);
        redis.setDebug(true);
        redis.setApplication("TestStream");
        boolean result = redis.tryLock(taskId);
        Assert.assertTrue(result);
        result = redis.tryLock(taskId);
        Assert.assertFalse(result);
        String time = redis.get(taskId + "_lock");
        Assert.assertTrue(System.currentTimeMillis() - Long.parseLong(time) >= 0);
        redis.releaseLock(taskId);
        time = redis.get(taskId + "_lock");
        Assert.assertNull(time);
    }

    @Test(groups = "ignore")
    public void testSuspend() {
        TaskPersisterRedisImpl redis = new TaskPersisterRedisImpl();
        redis.setDebug(true);
        redis.setApplication("TestStream");
        redis.setRedisService(redisService);
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        redis.suspend(task, 10);
        assertNull(redis.get(task.getTaskId() + "_lock"));
        Set<Tuple> result = redisService.zrange("stream_auto_scheduled_retry_set_" + TaskPersisterRedisImpl.class.getSimpleName() + "TestStream", 0, 100);
        assertTrue(result.parallelStream().anyMatch(tuple -> {
            return tuple.getScore() == 100 && tuple.getElement().equals(task.getTaskId());
        }));
        assertNotNull(redisService.get(task.getTaskId()));
        redis.complete(task);
    }

    @Test(groups = "ignore")
    public void testComplete() {
        TaskPersisterRedisImpl redis = new TaskPersisterRedisImpl();
        redis.setDebug(true);
        redis.setApplication("TestStream");
        redis.setRedisService(redisService);
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        redis.suspend(task, 10);
        redis.complete(task);
        assertNull(redis.get(task.getTaskId() + "_lock"));
        assertNull(redisService.get(task.getTaskId()));
        Set<Tuple> result = redisService.zrange("stream_auto_scheduled_retry_set_" + TaskPersisterRedisImpl.class.getSimpleName() + "TestStream", 0, 100);
        assertFalse(result.parallelStream().anyMatch(tuple -> {
            return tuple.getScore() == 100 && tuple.getElement().equals(task.getTaskId());
        }));

    }

    @Test(groups = "ignore")
    public void testRemove() {
        redisService.del("stream_auto_scheduled_retry_set_" + TaskPersisterRedisImpl.class.getSimpleName() + "TestStream");
        redisService.del("stream_auto_scheduled_retry_set_" + TaskPersisterRedisImpl.class.getSimpleName());
        redisService.del("stream_auto_scheduled_backup_set_" + TaskPersisterRedisImpl.class.getSimpleName() + "TestStream");
        redisService.del("stream_auto_scheduled_backup_set_" + TaskPersisterRedisImpl.class.getSimpleName());

    }
}

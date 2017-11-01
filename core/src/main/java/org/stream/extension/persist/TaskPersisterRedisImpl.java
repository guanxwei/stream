package org.stream.extension.persist;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.stream.core.exception.RedisException;
import org.stream.extension.clients.AssembledShardedJedis;
import org.stream.extension.meta.Task;

import lombok.Setter;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.Tuple;

/**
 * {@link TaskPersister}'s default implementation.
 * @author hzweiguanxiong
 *
 */
public class TaskPersisterRedisImpl implements TaskPersister {

    private static final String RETRY_KEY = "stream_auto_scheduled_retry_set_" + TaskPersisterRedisImpl.class.getSimpleName();
    private static final String BACKUP_KEY = "stream_auto_scheduled_backup_set_" + TaskPersisterRedisImpl.class.getSimpleName();

    @Setter
    private RedisService redisService;

    @Setter
    private TaskDao taskDao;

    @Setter
    private String application;

    @Setter
    private boolean debug = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean persist(final Task task) {
        return taskDao.persist(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mark(final Task task, final String pattern, final int time) {
        assert application != null;

        double score = System.currentTimeMillis() + time * 1000;
        if (debug) {
            // To make sure Unit test cases can be run quickly.
            score = System.currentTimeMillis();
        }
        AssembledShardedJedis jedis = null;
        try {
            jedis = redisService.getJedis();
            ShardedJedisPipeline pipeline = jedis.getMasterShardedRedis().pipelined();
            pipeline.zadd(RETRY_KEY + application, score, task.getTaskId());
            pipeline.set(task.getTaskId(), task.toString());
            pipeline.sync();
            redisService.returnResource(jedis);
        } catch (Exception e) {
            redisService.returnBrokenResource(jedis);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String get(final String key) {
        return redisService.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unmark(final Task task) {
        assert application != null;

        AssembledShardedJedis jedis = null;
        try {
            jedis = redisService.getJedis();
            ShardedJedisPipeline pipeline = jedis.getMasterShardedRedis().pipelined();
            pipeline.del(task.getTaskId());
            pipeline.zrem(RETRY_KEY + application, task.getTaskId());
            pipeline.sync();
            redisService.returnResource(jedis);
        } catch (Exception e) {
            redisService.returnBrokenResource(jedis);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getPendingList(final int type) {
        assert application != null;

        if (type == 1) {
            Set<Tuple> result = redisService.zrange(RETRY_KEY + application, 0, 1000);
            return result.parallelStream()
                    .filter(tuple -> {
                        return System.currentTimeMillis() >= tuple.getScore();
                    })
                    .map(tuple -> {
                        return tuple.getElement();
                    })
                    .collect(Collectors.toList());
        } else if (type == 2) {
            return redisService.lrange(BACKUP_KEY + application, 0, 100);
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryLock(final String taskId) {
        boolean result = redisService.setnx(taskId + "_lock", Long.toString(System.currentTimeMillis())) == 1;
        if (result) {
            return true;
        } else {
            String time = redisService.get(taskId + "_lock");
            long realTime = Long.parseLong(time);
            long now = System.currentTimeMillis();
            // If the lock was hold longer then 10 minutes, we would think that the previous owner has crashed.
            if (now - realTime >= 600 * 1000) {
                //The task has been locked too long, the previous job is crashed.
                redisService.set(taskId + "_lock", Long.toString(System.currentTimeMillis()));
                return true;
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean releaseLock(final String taskId) {
        return redisService.del(taskId + "_lock");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setHub(final String taskId, final String content) {
        assert application != null;

        AssembledShardedJedis jedis = null;
        try {
            jedis = redisService.getJedis();
            ShardedJedisPipeline pipeline = jedis.getMasterShardedRedis().pipelined();
            pipeline.set(taskId + "_lock", Long.toString(System.currentTimeMillis()));
            pipeline.lpush(BACKUP_KEY + application, taskId);
            pipeline.set(taskId, content);
            pipeline.sync();
            redisService.returnResource(jedis);
        } catch (Exception e) {
            redisService.returnBrokenResource(jedis);
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeHub(final String taskId) {
        assert application != null;

        return redisService.lrem(BACKUP_KEY + application, 1, taskId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateTime(final String taskId) {
        return redisService.set(taskId + "_lock", Long.toString(System.currentTimeMillis()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspend(final Task task, final int time) {
        AssembledShardedJedis jedis = null;
        try {
            double score = System.currentTimeMillis() + time * 1000;

            if (debug) {
                // To make sure Unit test cases can be run quickly.
                score = 100;
            }

            jedis = redisService.getJedis();
            ShardedJedisPipeline pipeline = jedis.getMasterShardedRedis().pipelined();
            pipeline.del(task.getTaskId() + "_lock");
            pipeline.lrem(BACKUP_KEY + application, 1, task.getTaskId());
            pipeline.zadd(RETRY_KEY + application, score, task.getTaskId());
            pipeline.set(task.getTaskId(), task.toString());
            pipeline.sync();
            redisService.returnResource(jedis);
        } catch (Exception e) {
            redisService.returnBrokenResource(jedis);
            throw new RedisException("Error", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete(final Task task) {
        AssembledShardedJedis jedis = null;
        try {

            jedis = redisService.getJedis();
            ShardedJedisPipeline pipeline = jedis.getMasterShardedRedis().pipelined();
            pipeline.del(task.getTaskId() + "_lock");
            pipeline.del(task.getTaskId());
            pipeline.zrem(RETRY_KEY + application, task.getTaskId());
            pipeline.lrem(BACKUP_KEY + application, 1, task.getTaskId());
            pipeline.sync();
            redisService.returnResource(jedis);
        } catch (Exception e) {
            redisService.returnBrokenResource(jedis);
            throw new RedisException("Error", e);
        }
    }
}

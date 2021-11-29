/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stream.extension.lock.providers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import javax.annotation.Resource;

import org.stream.extension.clients.RedisClient;
import org.stream.extension.lock.Lock;
import org.stream.extension.settings.Settings;
import org.stream.extension.utils.actionable.Tellme;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * A special implement version of {@link Lock}, uses redis cluster as the underlying storage of the lock information.
 * @author guanxiongwei
 *
 */
@Slf4j
public class RedisClusterBasedLock implements Lock {

    private Map<String, String> processingTasks = new HashMap<>();
    private Map<String, Long> lockingTimes = new HashMap<>();

    @Resource
    @Setter
    private RedisClient redisClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryLock(final String key, final BiFunction<String, Long, Boolean> postAction) {
        long current = System.currentTimeMillis();
        boolean ownered = Thread.currentThread().getName().equals(processingTasks.get(key));

        // The lock was grabbed by this thread in the previous step, just skip the procedure or refresh the lock time.
        if (ownered && current - lockingTimes.get(key) < Settings.LOCK_EXPIRE_TIME) {
            if (current - lockingTimes.get(key) > Settings.LOCK_EXPIRE_TIME / 2) {
                // refresh locked time if we have hold the lock for a long time
                String expectedValue = genLockValue(lockingTimes.get(key));
                boolean refreshed = redisClient.updateKeyExpireTimeIfMatch(genLock(key), expectedValue);
                if (refreshed) {
                    lockingTimes.put(key, current);
                    log.info("Lock info refreshed");
                } else {
                    log.warn("Fail refreshing the lock expire time, the lock could be expired and transfered to ather workers");
                    return false;
                }
            }

            return true;
        }

        // The locked was grabbed by another thread in the same JVM.
        if (isProcessing(key) && !ownered) {
            log.info("Another thread in the jvm is processing the task, skip");
            // Duplicate thread in the same host.
            long lockingTime = lockingTimes.get(key);
            if (current - lockingTime >= Settings.LOCK_EXPIRE_TIME) {
                // The owner thread must be crashed or stuck, and the lock must be expired or refreshed by other workers.
                // Try to grab the lock, if succeed, kick off the previous owner.
                log.warn("The processing thread must be crashed or blocked by some actions, will try to grab the lock");
                return requireLock(key, current, postAction);
            }
            return false;
        }

        return requireLock(key, current, postAction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean release(final String key) {
        processingTasks.remove(key);
        Long lockTime = lockingTimes.remove(key);
        // Lock for to long time, let other workers to release the lock.
        if (lockTime != null && System.currentTimeMillis() - lockTime > Settings.LOCK_EXPIRE_TIME) {
            return true;
        }

        return redisClient.del(genLock(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLegibleOwner(final String taskId) {
        String ownerInfo = redisClient.get(genLock(taskId));
        if (ownerInfo == null) {
            return true;
        }

        return ownerInfo.startsWith(Settings.HOST_NAME);
    }

    private String genLockValue(final long current) {
        return Settings.HOST_NAME + "_" + current;
    }

    private String genLock(final String taskId) {
        return taskId + "_lock";
    }

    private boolean isProcessing(final String taskId) {
        return processingTasks.containsKey(taskId);
    }

    private boolean requireLock(final String taskId, final long currentTime, final BiFunction<String, Long, Boolean> postAction) {
        String threadName = Thread.currentThread().getName();
        log.info("Try to grab the lock for task [{}] at time [{}] by thread [{}]", taskId, currentTime, threadName);
        boolean locked = redisClient.setnxWithExpireTime(genLock(taskId), genLockValue(currentTime)) == 1L;

        Tellme.when(locked).then(() -> {
            log.info("Thread [{}] Grab the lock for task [{}]", Thread.currentThread().getName(), taskId);
            lockingTimes.put(taskId, currentTime);
            processingTasks.put(taskId, threadName);
            postAction.apply(taskId, currentTime);
        });

        Tellme.when(!locked).then(() -> log.info("Another worker is processing the task"));

        return locked;
    }
}

package org.stream.extension.lock;

import java.util.function.BiFunction;

/**
 * Abstract of lock.
 * @author guanxiongwei
 *
 */
public interface Lock {

    /**
     * Try to grab the lock for the specific task.
     * @param taskId Target task's identity.
     * @param postAction Action to be executed if the lock is successfully grabbed by current thread,
     *      if the lock has been grabbed by the thread within the legal time window then nothing will be executed.
     * @return {@code true} if succeeds, otherwise {@code false}
     */
    boolean tryLock(final String taskId, final BiFunction<String, Long, Boolean> postAction);

    /**
     * Release the lock for the target task.
     * @param taskId Target task's identity.
     * @return {@code true} if succeeds, otherwise {@code false}
     */
    boolean release(final String taskId);

    /**
     * Test if the lock was hold by current thread or no body has ever required the lock before.
     * @param taskId Target task's identity.
     * @return {@code true} if no-one holds the lock or the lock is hold by the current thread, otherwise {@code false}.
     */
    boolean isLegibleOwner(final String taskId);
}

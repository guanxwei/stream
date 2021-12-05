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

package org.stream.extension.lock;

import java.util.function.BiFunction;

/**
 * Abstract of lock.
 * @author guanxiongwei
 *
 */
public interface Lock {

    /**
     * Try to grab the lock for the specific key.
     * @param key lock key.
     * @param postAction Action to be executed if the lock is successfully grabbed by current thread,
     *      if the lock has been grabbed by the thread within the legal time window then nothing will be executed.
     * @return {@code true} if succeeds, otherwise {@code false}
     */
    boolean tryLock(final String key, final BiFunction<String, Long, Boolean> postAction);

    /**
     * Release the lock for the target key.
     * @param key lock key.
     * @return {@code true} if succeeds, otherwise {@code false}
     */
    boolean release(final String key);

    /**
     * Test if the lock was hold by current thread or no body has ever required the lock before.
     * @param key lock key.
     * @return {@code true} if no-one holds the lock or the lock is hold by the current thread, otherwise {@code false}.
     */
    boolean isLegibleOwner(final String key);
}

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

package org.stream.extension.utils.actionable.state;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.stream.extension.utils.actionable.operation.ExceptionOperation;
import org.stream.extension.utils.actionable.operation.Operation;

/**
 * Exceptional state.
 * @author guanxiongwei
 *
 */
public interface ExceptionalState {

    public static final ThreadLocal<Set<Class<? extends Exception>>> TARGETS = new ThreadLocal<>();

    /**
     * Register interested exceptions.
     * @param targets interested exception list.
     * @return Enhanced state.
     */
    @SuppressWarnings("unchecked")
    default ExceptionalState incase(final Class<? extends Exception>... targets) {
        TARGETS.set(new HashSet<>());
        TARGETS.get().addAll(Arrays.asList(targets));
        return this;
    }

    /**
     * Fix the exception, and users can keep on adding more exceptional cases and fix solutions.
     * @param operation Fix operation.
     * @return Exceptional state so that users can keep adding more exceptional cases.
     */
    ExceptionalState thenFix(final ExceptionOperation operation);

    /**
     * Do something before we leave the tellme especially if there is a new exception thrown from the fixes.
     * 
     * @param operation Operation to be executed before we leave the tellme statement.
     */
    void reagardless(final Operation operation);
}
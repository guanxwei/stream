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

import org.stream.extension.utils.actionable.Tellme;
import org.stream.extension.utils.actionable.exception.FixException;
import org.stream.extension.utils.actionable.operation.ExceptionOperation;
import org.stream.extension.utils.actionable.operation.Operation;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception state.
 * @author guanxiongwei
 *
 */
@Slf4j
public class ExceptionState implements ExceptionalState {

    private final Exception cause;

    /**
     * Constructor.
     * @param e Exception.
     */
    public ExceptionState(final Exception e) {
        this.cause = e;
    }

        /**
     * {@inheritDoc}
     */
    @Override
    public ExceptionalState thenFix(ExceptionOperation operation) {
        if (Tellme.CACHED_EXCEPTION.get() != null) {
            // Another worker has done its job, we should pass the work to the regardless method.
            return this;
        }
        if (Tellme.HAVING_FINALLY.get()) {
            // we should run in safe mode, cache the exception here so that regardless method has a chance to do it's work.
            try {
                return fix(operation);
            } catch(Throwable t) {
                Tellme.CACHED_EXCEPTION.set(t);
                return this;
            }
        } else {
            return fix(operation);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reagardless(final Operation operation) {
        operation.operate();
        Throwable t = Tellme.CACHED_EXCEPTION.get();
        if (t != null) {
            Tellme.CACHED_EXCEPTION.remove();
            throw new FixException(t);
        }
    }

    private boolean contains(final Exception e) {
        if (TARGETS.get().contains(e.getClass())) {
            return true;
        }
        for (Class<?> target : TARGETS.get()) {
            if (target.isAssignableFrom(e.getClass())) {
                return true;
            }
        }
        return false;
    }

    private ExceptionalState fix(ExceptionOperation operation) {
        // The cause is in the interested list, then do something.
        if (contains(cause)) {
            operation.fix(this.cause);
            // Clear the context, so that later fixes have a chance to fix the exception
            TARGETS.remove();
            return new ExceptionState(cause);
        }

        // If we cannot fix the exception, just let it go.
        return this;
    }
}

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

    private Exception cause;

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
    public void fix(final ExceptionOperation operation) {
        // The cause is in the interested list, then do something.
        if (contains(cause)) {
            operation.fix(this.cause);
            return;
        }

        if (this.cause instanceof RuntimeException) {
            throw (RuntimeException)this.cause;
        }

        throw new RuntimeException(this.cause);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void anyway(final Operation fix, final Operation remedy) {
        try {
            fix.operate();
        } catch (Exception e) {
            try {
                remedy.operate();
            } catch (Exception e0) {
                log.error("Exception should not be thrown in the remedy action, now we catch it but not throw it", e);
            }
            throw e;
        }
    }
}

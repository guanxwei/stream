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

/**
 * Exception state.
 * @author guanxiongwei
 *
 */
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
        if (TARGETS.get().contains(cause.getClass())) {
            operation.fix(this.cause);
            return;
        }

        if (this.cause instanceof RuntimeException) {
            throw (RuntimeException)this.cause;
        }

        throw new RuntimeException(this.cause);
    }
}

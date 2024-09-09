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

package org.stream.extension.utils.actionable.exception;

import org.stream.extension.utils.actionable.state.ExceptionalState;

/**
 * Exception will only be thrown from the {@linkplain ExceptionalState#reagardless(org.stream.extension.utils.actionable.operation.Operation)}.
 * It will wrap the exceptions thrown from the fixes.
 * 
 * @author guanxiongwei
 * @since 09/09/2024
 */
public class FixException extends RuntimeException {

    /**
     * Constructor with throwable info from fixes.
     * @param t throwable info from fixes.
     */
    public FixException(final Throwable t) {
        super(t);
    }
}
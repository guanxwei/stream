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

package org.stream.extension.utils.actionable.operation;

import org.stream.extension.utils.actionable.Tellme;
import org.stream.extension.utils.actionable.state.ExceptionState;

/**
 * Special operation only suitable for {@link ExceptionState}.
 * @author guanxiongwei
 *
 */
public interface ExceptionOperation {

    /**
     * Try to fix the arisen exception.
     * @param e Exception thrown when invoked the {@link Tellme#tryIt(Risk)}.
     */
    void fix(final Exception e);
}

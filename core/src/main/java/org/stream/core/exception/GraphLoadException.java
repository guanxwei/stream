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

package org.stream.core.exception;

import java.io.Serial;

/**
 * Graph load exception.
 *
 */
public class GraphLoadException extends StreamException {

    @Serial
    private static final long serialVersionUID = 6245569938672328171L;

    // CHECKSTYLE:OFF
    public GraphLoadException(final String message) {
        super(message);
    }

    public GraphLoadException(final String message, final Throwable t) {
        super(message, t);
    }

    public GraphLoadException(final Throwable t) {
        super(t);
    }
    // CHECKSTYLE:ON
}

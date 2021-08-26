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

package org.stream.core.execution;

/**
 * Utility class to provide some basic configuration option.
 *
 */
public final class RetryPattern {

    private RetryPattern() { }

    /**
     * Default stuck work-flow retry pattern.
     */
    public static final String EQUAL_DIFFERENCE = "EqualDifference";

    /**
     * Advanced pattern to re-run stuck work-flows at schedule time.
     */
    public static final String SCHEDULED_TIME = "ScheduledTime";

}

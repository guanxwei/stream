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

package org.stream.extension.pattern;

import java.util.concurrent.TimeUnit;

import org.stream.core.execution.AutoScheduledEngine;

/**
 * Retry pattern used to generate time interval to elapse before {@link AutoScheduledEngine} retry
 * the stuck work-flow instance.
 * @author guanxiong wei
 *
 */
public interface RetryPattern {

    /**
     * Generate time interval in {@link TimeUnit#MILLISECONDS} based on the current retry times, {@link AutoScheduledEngine} will retry
     * failed activity at most 24 times.
     *
     * To eliminate condition, at least 10 milliseconds should be specified.
     * @param retryTime Retry times.
     * @return Time interval.
     */
    int getTimeInterval(final int retryTime);
}

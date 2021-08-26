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

package org.stream.extension.pattern.defaults;

import org.stream.extension.pattern.RetryPattern;

import com.google.common.collect.ImmutableList;

/**
 * Progressive increase time interval retry pattern.
 * @author guanxiong wei
 *
 */
public class ScheduledTimeIntervalPattern implements RetryPattern {

    private static final ImmutableList<Integer> SCHEDULED = ImmutableList.<Integer>builder()
            .add(1000)
            .add(1000)
            .add(1000)
            .add(1000)
            .add(1000)
            .add(5000)
            .add(5000)
            .add(5000)
            .add(5000)
            .add(10000)
            .add(10000)
            .add(10000)
            .add(20000)
            .add(20000)
            .add(30000)
            .add(30000)
            .add(240000)
            .add(300000)
            .add(1200000)
            .add(1800000)
            .add(7200000)
            .add(25200000)
            .add(36000000)
            .add(14400000)
            .add(14400000)
            .add(14400000)
            .build();

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTimeInterval(final int retryTime) {
        return SCHEDULED.get(retryTime);
    }

}

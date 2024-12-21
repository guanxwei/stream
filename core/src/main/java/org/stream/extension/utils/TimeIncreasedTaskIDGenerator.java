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

package org.stream.extension.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.stream.core.resource.Resource;

/**
 * Snowflake version task id generator.
 * Constructed by time in milliseconds + counts + random key.
 * The length will be kept at 26.
 * 
 * @author weiguanxiong
 * @since 2024/09/01
 */
public class TimeIncreasedTaskIDGenerator implements TaskIDGenerator {

    private static final AtomicLong COUNTER = new AtomicLong(0);

    private static final String RANDOM = UUID.randomUUID().toString().substring(0, 6);

    /**
     * {@inheritDoc}}
     */
    @Override
    public String generateTaskID(final Resource primary) {
        long now = System.currentTimeMillis();
        DateFormat format = new SimpleDateFormat("yyyyMMddhhmmssSSS");
        return format.format(new Date(now)) +
                RANDOM +
                COUNTER.addAndGet(1) % 1000;
    }
}

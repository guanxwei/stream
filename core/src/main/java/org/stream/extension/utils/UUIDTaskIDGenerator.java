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

import java.util.UUID;

import org.stream.core.resource.Resource;

/**
 * A {@link TaskIDGenerator} implementation to generate task id
 * using UUID facility. Ignore the input primary resource.
 * @author weiguanxiong
 *
 */
public class UUIDTaskIDGenerator implements TaskIDGenerator {

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateTaskID(final Resource primary) {
         return UUID.randomUUID().toString();
    }

}

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

package org.stream.extension.persist;

import org.stream.extension.clients.RedisClientImpl;

import lombok.Setter;

/**
 * Encapsulation of Redis Service.
 * Provide some valuable APIs to manufacture data stored in Redis cluster.
 * @author guanxiong wei
 *
 */
@Setter
public class RedisService extends RedisClientImpl {

    private int maxActive = 50;

    private int maxIdle = 30;

    private int minIdle = 5;

    /**
     * Initiate method to initiate Redis clients pool.
     * @param nodes Cluster node list.
     * @param timeout Timeout.
     * @param maxRetryTimes Max retry times.
     */
    public RedisService(final String nodes, final int timeout, final int maxRetryTimes) {
        super.setNodes(nodes);
        super.setTimeout(timeout);
        super.setMaxActive(maxActive);
        super.setMaxIdle(maxIdle);
        super.setMinIdle(minIdle);
        super.setMaxRetryTimes(maxRetryTimes);
        super.init();
    }

}

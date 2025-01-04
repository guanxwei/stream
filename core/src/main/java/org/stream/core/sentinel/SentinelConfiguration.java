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

package org.stream.core.sentinel;

import lombok.Data;

/**
 * Sentinel configuration at node level.
 */
@Data
public class SentinelConfiguration {

    // Throttle type, by exceptional rate or rt.
    private String type;

    // Ratio threshold
    private float ratioThreshold;

    // Time window.
    private int timeWindow;

    // Minimal requests;
    private int minimalRequests;

    // Degraded duration in milliseconds.
    private int duration;

    // Value threshold.
    private int valueThreshold;

    // The engine will rewrite this value even if the user give a name here, the loader will use the "{graphName}::{nodeName}" as the resource name.
    private String resourceName;

    /**
     *  Sentinel rule grade, for example for degrade strategy, 0 by rt, 1 by exceptional ratio, 2 by exception counts.
     *  For detail, please refer to the alibaba sentinel framework.
     */
    private int grade;

    // Default result, when the node is blocked by the sentinel framework, which activity result to be used by the engine, if it is not specified fail result will be used.
    private String defaultResult;
}

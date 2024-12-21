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

package org.stream.core.helper;

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
    private long timeWindow;

    // Minimal requests;
    private int minimalRequests;

    // Degraded duration.
    private int duration;

    // Value threshold.
    private int valueThreshold;
}

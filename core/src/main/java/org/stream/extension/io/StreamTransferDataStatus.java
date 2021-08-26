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

package org.stream.extension.io;

/**
 * Utility class providing standard Stream Transfer Data Status.
 * @author guanxiong wei
 *
 */
public final class StreamTransferDataStatus {

    private StreamTransferDataStatus() { }

    /**
     * Success result.
     */
    public static final String SUCCESS = "SUCCESS";

    /**
     * Fail result.
     */
    public static final String FAIL = "FAIL";

    /**
     * Result unknown after the node is executed, sometimes extra effort need to be taken to check if everything is okay.
     */
    public static final String UNKNOWN = "UNKNOWN";

    /**
     * Unknown result, maybe the current work flow should be suspended and tried again later.
     */
    public static final String SUSPEND = "SUSPEND";

    
}

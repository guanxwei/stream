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

package org.stream.extension.meta;

/**
 * Task status.
 * @author guanxiong wei
 *
 */
public enum TaskStatus {

    /**
     * Task initiated status.
     */
    INITIATED(0, "Initiated"),

    /**
     * Task being processed status.
     */
    PROCESSING(1, "Proccesing"),

    /**
     * Task processed normally.
     */
    COMPLETED(3, "Completed"),

    /**
     * Task failed.
     */
    FAILED(4, "Failed"),

    /**
     * Suspended status, will be retried later.
     */
    PENDING(5, "PendingOnRetry");

    private int code;
    private String type;

    private TaskStatus(final int code, final String type) {
        this.code = code;
        this.type = type;
    }

    public int code() {
        return this.code;
    }

    public String type() {
        return this.type;
    }
}

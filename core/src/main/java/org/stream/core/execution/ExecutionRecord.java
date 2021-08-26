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

import java.util.Date;

import lombok.Builder;
import lombok.Getter;

/**
 * Encapsulation of workflow working record, which indicates what the workflow is doing.
 */
@Builder
public class ExecutionRecord {

    @Getter
    private Date time;

    @Getter
    private String description;

    /**
     * Print the record information.
     * @return Human friendly information about the record.
     */
    public String print() {
        return toString();
    }

    @Override
    public String toString() {
        return "This record is created at time : " + time.toString()
                + "/n"
                + "Detail description as below : " + description;
    }
}

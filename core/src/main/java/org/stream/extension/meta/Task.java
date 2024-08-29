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

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Abstract of an real task for one execution plan. Ths is used to track one scheduled work flow execution.
 * Every time users submit tasks to the atuo schedule engine.
 * 
 * @author guanxiong wei
 *
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Task implements Serializable {

    private static final long serialVersionUID = 7308719845192437440L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Task id, randomly generated by uuid.
    private String taskId;

    // Task status
    private int status;

    // Graph name.
    private String graphName;

    // Currently executed node.
    private String nodeName;

    // Millisecond represented time of the work-flow was previous executed.
    private long lastExcutionTime;

    // Jsonfied string represent of primary resource sent by the invoker at the initiate time.
    private String jsonfiedPrimaryResource;

    // Retry times at the current node.
    private int retryTimes;

    // Application the task belongs to.
    private String application;

    // Next time when the task should be reran.
    private long nextExecutionTime;

    // In case underlying DAO framework needs.
    private long id;

    // Time the task is initiated.
    private long initiatedTime;

    /**
     * Parse {@linkplain Task} entity from Jsonfied string.
     * @param content Jsonfied string.
     * @return Parsed {@linkplain Task} entity.
     */
    public static Task parse(final String content) {
        try {
            if (content == null) return null;
            return MAPPER.readValue(content, Task.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

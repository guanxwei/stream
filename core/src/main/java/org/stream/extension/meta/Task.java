package org.stream.extension.meta;

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Abstract of an real task for one execution plan.
 * @author hzweiguanxiong
 *
 */
@Data
@AllArgsConstructor
@Builder
public class Task implements Serializable {

    private static final long serialVersionUID = 7308719845192437440L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Default constructor.
     */
    public Task() { }

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

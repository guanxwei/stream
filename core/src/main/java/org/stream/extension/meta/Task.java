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

    private String taskId;

    private String status;

    private String graphName;

    private String nodeName;

    private long lastExcutionTime;

    private String jsonfiedTransferData;

    private String jsonfiedPrimaryResource;

    private int retryTimes;

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

    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

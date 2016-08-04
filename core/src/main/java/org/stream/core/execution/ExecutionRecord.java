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

package org.stream.extension.meta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Abstract of an real task for one execution plan.
 * @author guanxiong wei
 *
 */
@Data
@AllArgsConstructor
@Builder
public class TaskLock {

    private long id;

    private String taskId;

    private int version;

}

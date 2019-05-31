package org.stream.extension.meta;

import lombok.Data;

/**
 * Delayed queue.
 * @author weiguanxiong.
 *
 */
@Data
public class DelayedQueue {

    private long id;

    private String queueName;

    private String item;

    private long executionTime;

    private long addTime;
}

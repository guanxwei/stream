package org.stream.extension.meta;

/**
 * Task status.
 * @author weiguanxiong
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

package org.stream.core.execution;

/**
 * Utility class to provide some basic configuration option.
 *
 */
public final class RetryPattern {

    private RetryPattern() { }

    /**
     * Default stuck work-flow retry pattern.
     */
    public static final String EQUAL_DIFFERENCE = "EqualDifference";

    /**
     * Advanced pattern to re-run stuck work-flows at schedule time.
     */
    public static final String SCHEDULED_TIME = "ScheduledTime";

}

package org.stream.extension.pattern;

import java.util.concurrent.TimeUnit;

import org.stream.core.execution.AutoScheduledEngine;

/**
 * Retry pattern used to generate time interval to elapse before {@link AutoScheduledEngine} retry
 * the stuck work-flow instance.
 * @author guanxiong wei
 *
 */
public interface RetryPattern {

    /**
     * Generate time interval in {@link TimeUnit#MILLISECONDS} based on the current retry times, {@link AutoScheduledEngine} will retry
     * failed activity at most 24 times.
     *
     * To eliminate condition, at least 10 milliseconds should be specified.
     * @param retryTime Retry times.
     * @return Time interval.
     */
    int getTimeInterval(final int retryTime);
}

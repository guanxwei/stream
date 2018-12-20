package org.stream.extension.pattern.defaults;

import org.stream.extension.pattern.RetryPattern;

import com.google.common.collect.ImmutableList;

/**
 * 重试机制，时间间隔按照设定的值递增.
 * @author hzweiguanxiong
 *
 */
public class ScheduledTimeIntervalPattern implements RetryPattern {

    private static final ImmutableList<Integer> SCHEDULED = ImmutableList.<Integer>builder()
            .add(1000)
            .add(1000)
            .add(1000)
            .add(1000)
            .add(1000)
            .add(5000)
            .add(5000)
            .add(5000)
            .add(5000)
            .add(10000)
            .add(10000)
            .add(10000)
            .add(20000)
            .add(20000)
            .add(30000)
            .add(30000)
            .add(240000)
            .add(300000)
            .add(1200000)
            .add(1800000)
            .add(7200000)
            .add(25200000)
            .add(36000000)
            .add(14400000)
            .add(14400000)
            .add(14400000)
            .build();

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTimeInterval(final int retryTime) {
        return SCHEDULED.get(retryTime);
    }

}

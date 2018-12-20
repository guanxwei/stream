package org.stream.extension.pattern.defaults;

import org.stream.extension.pattern.RetryPattern;

import com.google.common.collect.ImmutableList;

/**
 * 失败重试机制，重试时间间隔等值，每隔10秒执行一次.
 * @author hzweiguanxiong
 *
 */
public class EqualTimeIntervalPattern implements RetryPattern {

    private static final ImmutableList<Integer> EQUAL = ImmutableList.<Integer>builder()
            .add(1000)
            .build();

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTimeInterval(final int retryTime) {
        return EQUAL.get(0);
    }

}

package org.stream.extension.pattern.defaults;

import org.stream.extension.pattern.RetryPattern;

import com.google.common.collect.ImmutableList;

/**
 * Equal interval retry pattern.
 * @author guanxiong wei
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

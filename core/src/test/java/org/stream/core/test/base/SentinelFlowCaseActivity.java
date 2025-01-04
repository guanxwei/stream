package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;

import java.util.concurrent.atomic.AtomicInteger;

public class SentinelFlowCaseActivity extends Activity {

    public static final AtomicInteger COUNT = new AtomicInteger(0);

    /**
     * Perform an activity as part of a work-flow.
     *
     * @return The activity result.
     */
    @Override
    public ActivityResult act() {
        COUNT.incrementAndGet();
        return ActivityResult.SUCCESS;
    }
}

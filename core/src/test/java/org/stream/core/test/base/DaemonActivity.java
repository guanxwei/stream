package org.stream.core.test.base;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.AsyncActivity;
import org.stream.extension.utils.actionable.Tellme;

import java.util.concurrent.atomic.AtomicInteger;

public class DaemonActivity extends AsyncActivity {

    public static final AtomicInteger TAG = new AtomicInteger(0);

    /**
     * Perform an activity as part of a work-flow.
     *
     * @return The activity result.
     */
    @Override
    public ActivityResult act() {
        Tellme.tryIt(() -> Thread.sleep(100L));
        TAG.incrementAndGet();
        return ActivityResult.SUCCESS;
    }
}

package org.stream.core.test.base;

import lombok.extern.slf4j.Slf4j;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.AsyncActivity;
import org.stream.extension.utils.actionable.Tellme;

@Slf4j
public class BlockAsyncActivity extends AsyncActivity {
    /**
     * Perform an activity as part of a work-flow.
     *
     * @return The activity result.
     */
    @Override
    public ActivityResult act() {
        Tellme.tryIt(() -> Thread.sleep(200));
        return null;
    }
}

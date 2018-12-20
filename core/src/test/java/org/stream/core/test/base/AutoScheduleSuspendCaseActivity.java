package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;

public class AutoScheduleSuspendCaseActivity extends Activity {

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityResult act() {
        throw new RuntimeException();
    }

}

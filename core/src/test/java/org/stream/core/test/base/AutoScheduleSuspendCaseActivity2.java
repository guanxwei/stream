package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.WorkFlowContext;

public class AutoScheduleSuspendCaseActivity2 extends Activity {

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityResult act() {
        WorkFlowContext.markException(new RuntimeException());
        return ActivityResult.SUSPEND;
    }

}

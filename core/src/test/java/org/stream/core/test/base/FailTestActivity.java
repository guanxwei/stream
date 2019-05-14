package org.stream.core.test.base;

import java.util.Date;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.WorkFlowContext;

public class FailTestActivity extends Activity {

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityResult act() {
        WorkFlowContext.keepRecord(ExecutionRecord.builder()
                .description("keep a success record for FailTestActivity")
                .time(new Date(System.currentTimeMillis() + 100))
                .build());
        return ActivityResult.FAIL;
    }

}

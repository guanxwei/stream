package org.stream.core.test.base;

import java.util.Date;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.WorkFlowContext;

public class SuccessTestActivity extends Activity {

    @Override
    public ActivityResult act() {
        WorkFlowContext.keepRecord(ExecutionRecord.builder()
                .description("keep a success record for SuccessTestActivity")
                .time(new Date(System.currentTimeMillis() + 100))
                .build());
        return ActivityResult.SUCCESS;
    }

}

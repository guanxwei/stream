package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.WorkFlowContext;

public class SuccessTestActivity extends Activity {

    @Override
    public ActivityResult act() {
        WorkFlowContext.keepRecord(ExecutionRecord.builder()
                .description("keep a success record")
                .time(null)
                .build());
        return ActivityResult.SUCCESS;
    }

}

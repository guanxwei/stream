package org.stream.core.test.base;

import java.util.Date;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;
import org.stream.core.resource.TimeOut;

public class SuspendActivity extends Activity {

    @Override
    public ActivityResult act() {
        Resource timeOut = Resource.builder()
                .resourceReference(TimeOut.TIME_OUT_REFERENCE)
                .value(400L)
                .build();
        WorkFlowContext.keepRecord(ExecutionRecord.builder()
                .description("keep a success record for SuspendActivity")
                .time(new Date(System.currentTimeMillis() + 100))
                .build());
        WorkFlowContext.attachResource(timeOut);
        return ActivityResult.SUSPEND;
    }

}

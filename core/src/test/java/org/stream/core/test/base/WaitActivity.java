package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.WorkFlowContext;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class WaitActivity extends Activity {

    /**
     * Perform an activity as part of a work-flow.
     *
     * @return The activity result.
     */
    @Override
    public ActivityResult act() {
        try {
            waitUntilAsyncWorksFinished("waitNode", 300);
            WorkFlowContext.keepRecord(ExecutionRecord.builder()
                    .description("WaitExpiredSuccess")
                    .build());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            WorkFlowContext.keepRecord(ExecutionRecord.builder()
                    .description("WaitExpiredFailed")
                    .build());
        }
        return ActivityResult.SUCCESS;
    }
}

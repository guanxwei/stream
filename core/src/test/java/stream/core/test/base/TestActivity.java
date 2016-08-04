package stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;

public class TestActivity extends Activity {

    @Override
    public ActivityResult act() {
        WorkFlowContext.keepRecord(ExecutionRecord.builder()
                .description("keep a record")
                .time(null)
                .build());
        Resource primarySource = WorkFlowContext.getPrimary();
        if (primarySource != null) {
            primarySource.setValue(Long.valueOf("100000"));
        }
        return ActivityResult.SUCCESS;
    }

}

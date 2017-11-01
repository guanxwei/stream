package org.stream.extension;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;

public class RetryActivity extends Activity {

    @Override
    public ActivityResult act() {
        Resource primary = WorkFlowContext.getPrimary();
        StreamTransferData data = (StreamTransferData) primary.getValue();
        if (data.get("Count") == null) {
            System.out.println("I choose to sleep");
            data.set("Count", "Hello");
            return ActivityResult.SUSPEND;
        } else {
            System.out.println("I have slept for too much time, this time i choose to work");
        }
        return ActivityResult.SUCCESS;
    }

}

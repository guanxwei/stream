package org.stream.core.test.base;

import java.util.LinkedList;
import java.util.List;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;

public class PrintRecordActivity extends Activity {

    @Override
    public ActivityResult act() {
        List<ExecutionRecord> executionRecords = new LinkedList<>();
        for (ExecutionRecord executionRecord : WorkFlowContext.getRecords()) {
            executionRecords.add(executionRecord);
        }

        WorkFlowContext.attachResource(Resource.builder()
                .resourceReference("PrintRecordActivity")
                .value(executionRecords)
                .build());
        return ActivityResult.SUCCESS;
    }

}

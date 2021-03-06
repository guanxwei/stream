package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.DefaultEngine;
import org.stream.core.execution.Engine;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.WorkFlowContext;

import lombok.Setter;

public class CascadeTestActivity extends Activity {

    @Setter
    private GraphContext graphContext;

    @Override
    public ActivityResult act() {
        String graphName = "comprehensive";
        Engine engine = new DefaultEngine();
        WorkFlowContext.keepRecord(ExecutionRecord.builder()
                .description("keep a cascade record")
                .time(null)
                .build());
        engine.execute(graphContext, graphName, false);

        return ActivityResult.SUCCESS;
    }

}

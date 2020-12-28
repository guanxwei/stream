package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.WorkFlowContext;

public class ConditionActivity extends Activity {

    @Override
    public ActivityResult act() {
        WorkFlowContext.makrCondition(1);
        return ActivityResult.CONDITION;
    }
    
}

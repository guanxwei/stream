package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;

public class ConditionActivity extends Activity {

    @Override
    public ActivityResult act() {
        return ActivityResult.condition(1);
    }
    
}

package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;

public class ExceptionThrownSuccesorActivity extends Activity {

    @Override
    public ActivityResult act() {
        Resource resource = WorkFlowContext.resolveResource("TestExceptionThrownCase");
        System.out.println("Resource == null is " + (resource == null));
        System.out.println(Thread.currentThread().getName());
        return ActivityResult.SUCCESS;
    }

}

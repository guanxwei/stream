package org.stream.core.test.base;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;

/**
 * Activity to mock scenarios that Exception will be thrown during processing.
 * @author hzweiguanxiong
 *
 */
public class ExceptionThrownActivity extends Activity {

    @Override
    public ActivityResult act() {
        Resource resource = Resource.builder()
                .value("Exception")
                .resourceReference("TestExceptionThrownCase")
                .build();
        WorkFlowContext.attachResource(resource);
        System.out.println(Thread.currentThread().getName());
        throw new RuntimeException();
    }

}

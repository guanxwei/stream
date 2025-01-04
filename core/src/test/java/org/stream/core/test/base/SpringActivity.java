package org.stream.core.test.base;

import javax.annotation.Resource;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.runtime.test.SpringModule;

public class SpringActivity extends Activity {

    @Resource
    private SpringModule springModule;

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityResult act() {
        WorkFlowContext.attachResource(org.stream.core.resource.Resource.builder()
                .resourceReference("springModule")
                .value(springModule)
                .build());
        return ActivityResult.SUCCESS;
    }

}

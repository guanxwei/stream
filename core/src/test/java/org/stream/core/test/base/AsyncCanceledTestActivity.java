package org.stream.core.test.base;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.AsyncActivity;
import org.stream.core.resource.Resource;

public class AsyncCanceledTestActivity extends AsyncActivity {

    @Override
    public ActivityResult act() {
        try {
            addResource(Resource.builder()
                    .value("asyncvalue")
                    .resourceReference("asyncreference")
                    .build());
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            return ActivityResult.FAIL;
        }
        cleanUp();
        return ActivityResult.SUCCESS;
    }

}

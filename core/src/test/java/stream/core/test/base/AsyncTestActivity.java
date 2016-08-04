package stream.core.test.base;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.AsyncActivity;
import org.stream.core.resource.Resource;

public class AsyncTestActivity extends AsyncActivity {

    @Override
    public ActivityResult act() {
        try {
            addResource(Resource.builder()
                    .value("asyncvalue")
                    .resourceReference("asyncreference")
                    .build());
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ActivityResult.SUCCESS;
    }

}

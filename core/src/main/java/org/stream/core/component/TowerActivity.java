package org.stream.core.component;

import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.Tower;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Special activity containing a {@link Tower} instance to do the real work.
 *
 * Users should not use this class themselves, the framework will help construct a instance of this
 * type when the activityClass is implementation of {@link Tower} in graph definition.
 * @author hzweiguanxiong
 *
 */
@Slf4j
public class TowerActivity extends Activity {

    @Setter @Getter
    private Tower tower;

    public TowerActivity(final Tower tower) {
        this.tower = tower;
    }

    public TowerActivity() {
        this(null);
    }

    /**
     * {@inheritDoc}
     */
    public ActivityResult act() {
        if (tower == null) {
            throw new WorkFlowExecutionExeception("Actor must be specified");
        }

        StreamTransferData streamTransferData = null;
        try {
            Resource resource = WorkFlowContext.resolveResource(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE);
            StreamTransferData contextData = resource.resolveValue(StreamTransferData.class);
            streamTransferData = tower.call(contextData);
            StreamTransferData.merge(contextData, streamTransferData);
            return ActivityResult.valueOf(streamTransferData.getActivityResult());
        } catch (Exception e) {
            log.error("Fail to call actor [{}]", tower.getClass().getName());
            Resource resource = WorkFlowContext.resolveResource(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE);
            StreamTransferData contextData = resource.resolveValue(StreamTransferData.class);
            streamTransferData = StreamTransferData.failed();
            StreamTransferData.merge(contextData, streamTransferData);
            e.printStackTrace();
            return ActivityResult.SUSPEND;
        }
    }
}

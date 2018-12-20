package org.stream.core.component;

import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;
import org.stream.extension.io.Actor;
import org.stream.extension.io.StreamTransferData;

import lombok.Getter;
import lombok.Setter;

/**
 * Special activity containing a {@link actor} instance to do the real work.
 * @author hzweiguanxiong
 *
 */
public abstract class ActorActivity<T> extends Activity {

    @Setter @Getter
    private Actor<T> actor;

    public ActorActivity(final Actor<T> actor) {
        this.actor = actor;
    }

    public ActorActivity() {
        this(null);
    }

    /**
     * {@inheritDoc}
     */
    public ActivityResult act() {
        if (actor == null) {
            throw new WorkFlowExecutionExeception("Actor must be specified");
        }

        StreamTransferData streamTransferData = null;
        try {
            streamTransferData = actor.call(prepareRequest());
            Resource resource = WorkFlowContext.resolveResource(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE);
            StreamTransferData contextData = resource.resolveValue(StreamTransferData.class);
            StreamTransferData.merge(contextData, streamTransferData);
            return ActivityResult.valueOf(streamTransferData.getActivityResult());
        } catch (Exception e) {
            Resource resource = WorkFlowContext.resolveResource(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE);
            StreamTransferData contextData = resource.resolveValue(StreamTransferData.class);
            streamTransferData = StreamTransferData.failed();
            StreamTransferData.merge(contextData, streamTransferData);
            return ActivityResult.UNKNOWN;
        }
    }

    protected abstract T prepareRequest();
}

package org.stream.extension.assemble;

import java.util.List;

import org.stream.core.component.Activity;
import org.stream.core.exception.StreamException;
import org.stream.core.helper.NodeConfiguration;

import lombok.Getter;

/**
 * A procedure stub that is used to temporally store the step configuration for a {@link Procedure}, each step in
 * a procedure contains the instruction that the engine should do when the step is reached.
 *
 * Technically, each stub will be eventually translated into the {@link NodeConfiguration} so that this information can be
 * used by the stream work-flow engines and graph loaders.
 * @author weiguanxiong.
 *
 */
public class ProcedureStub {

    public static final int SUCCEED = 0;
    public static final int FAILED = 1;
    public static final int SUSPENED = 2;
    public static final int CHECKED = 3;

    private Procedure procedure;
    private int index = -100;
    @Getter
    private String[] nextSteps = new String[16];
    private boolean activityNeeded = false;
    @Getter
    private Activity action;
    @Getter
    private List<String> dependencies;

    public ProcedureStub(final Procedure procedure) {
        this.procedure = procedure;
    }

    public Procedure done() {
        this.procedure.addStub(this);
        return this.procedure;
    }

    public ProcedureStub when(final ProcedureCondition procedureCondition) throws StreamException {
        procedureCondition.onCondition(this);
        return this;
    }

    protected ProcedureStub whenSucceeded() throws StreamException {
        check();
        index = SUCCEED;
        return this;
    }

    protected ProcedureStub whenFailed() throws StreamException {
        check();
        index = FAILED;
        return this;
    }

    protected ProcedureStub whenSuspended() throws StreamException {
        check();
        index = SUSPENED;
        return this;
    }

    protected ProcedureStub whenChecked() throws StreamException {
        check();
        index = CHECKED;
        return this;
    }

    private void check() throws StreamException {
        if (activityNeeded) {
            throw new StreamException("A activity must be specified before configuring new condition steps");
        }
        activityNeeded = true;
    }

    public ProcedureStub act(final Activity activity) {
        this.action = activity;
        return this;
    }

    public ProcedureStub dependsOn(final List<String> activities) throws StreamException {
        this.dependencies = activities;
        return this;
    }

    public ProcedureStub then(final String action) {
        nextSteps[index] = action;
        activityNeeded = false;
        return this;
    }
}

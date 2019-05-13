package org.stream.extension.assemble;

import org.stream.core.exception.StreamException;

/**
 * Procedure condition.
 * @author weiguanxiong.
 *
 */
public enum ProcedureCondition {

    /**
     * Succeed condition.
     */
    SUCCEED {
        @Override
        protected void onCondition(final ProcedureStub procedureStub) throws StreamException {
            procedureStub.whenSucceeded();
        }
    },

    FAILED {
        @Override
        protected void onCondition(final ProcedureStub procedureStub) throws StreamException {
            procedureStub.whenFailed();
        }
    },

    SUSPENED {
        @Override
        protected void onCondition(final ProcedureStub procedureStub) throws StreamException {
            procedureStub.whenSuspended();
        }
    },

    CHECKED {
        @Override
        protected void onCondition(final ProcedureStub procedureStub) throws StreamException {
            procedureStub.whenChecked();
        }
    };

    protected abstract void onCondition(final ProcedureStub procedureStub) throws StreamException;
}

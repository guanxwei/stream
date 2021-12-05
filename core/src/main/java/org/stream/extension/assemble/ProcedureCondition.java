/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    },

    CONDITION {

        @Override
        protected void onCondition(final ProcedureStub procedureStub) throws StreamException {
            procedureStub.whenCondition();
        }
    },

    SUBFLOW {

        @Override
        protected void onCondition(final ProcedureStub procedureStub) throws StreamException {
            procedureStub.whenSubflow();
        }
    },

    INVOKE {

        @Override
        protected void onCondition(final ProcedureStub procedureStub) throws StreamException {
            procedureStub.whenSubflow();
        }
    };

    protected abstract void onCondition(final ProcedureStub procedureStub) throws StreamException;
}

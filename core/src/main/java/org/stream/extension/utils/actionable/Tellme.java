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

package org.stream.extension.utils.actionable;

import org.stream.extension.utils.actionable.operation.Risk;
import org.stream.extension.utils.actionable.state.ExceptionState;
import org.stream.extension.utils.actionable.state.ExceptionalState;
import org.stream.extension.utils.actionable.state.FalseState;
import org.stream.extension.utils.actionable.state.NormalState;
import org.stream.extension.utils.actionable.state.State;
import org.stream.extension.utils.actionable.state.TrueState;

/**
 * Tellme class. Just tell this class what to do when the condition fulfilles.
 * @author guanxiongwei
 *
 */
public final class Tellme {

    private Tellme() { }

    /**
     * Start a new state, return a state based on the input condition.
     * @param condition Condition.
     * @return New state.
     */
    public static State when(final boolean condition) {
        if (condition) {
            return new TrueState();
        }

        return new FalseState();
    }

    /**
     * Start a new state if the input two object equals returns a
     * true state otherwise return a false state.
     * @param real Object to be checked.
     * @param expected Target object.
     * @return New state.
     */
    public static State equals(final Object real, final Object expected) {
        if (expected != null & expected.equals(real)) {
            return new TrueState();
        }

        return new FalseState();
    }

    public static State whenNull(final Object real) {
        if (real == null) {
            return new TrueState();
        }

        return new FalseState();
    }

    public static ExceptionalState tryIt(final Risk risk) {
        try {
            risk.go();
            return new NormalState();
        } catch (Exception e) {
            return new ExceptionState(e);
        }
    }
}

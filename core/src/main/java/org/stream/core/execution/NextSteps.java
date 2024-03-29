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

package org.stream.core.execution;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Node;

import lombok.Setter;

/**
 * Encapsulation of the next step information used by the {@linkplain Engine} to determine what to do
 * after completing the work in the current node. In the steam work-flow world, the "work" to be done means the
 * {@linkplain Node} to be executed on.
 *
 * The work-flow engines will choose the next node based on the execution result of current node and the node's
 * next step configuration. Currently, stream framework supports 4 kinds of next step specification
 * <p> SUCCESS the success next step will be chosen when the current node returns {@link ActivityResult#SUCCESS}
 * <P> SUSPEND the success next step will be chosen when the current node returns {@link ActivityResult#SUSPEND}
 * <p> UNKNOW the success next step will be chosen when the current node returns {@link ActivityResult#UNKNOWN}
 * <p> FAIL the success next step will be chosen when the current node returns {@link ActivityResult#FAIL}
 *
 */
public class NextSteps {

    @Setter
    private Node success;

    @Setter
    private Node fail;

    @Setter
    private Node suspend;

    @Setter
    private Node check;

    /**
     * Return the successor {@link Node}, invoked only when the current {@link Node} returns
     * {@link ActivityResult#SUCCESS}.
     * @return The successor {@link Node}.
     */
    public Node onSuccess() {
        return success;
    }

    /**
     * Return the successor {@link Node}, invoked only when the current {@link Node} returns
     * {@link ActivityResult#FAIL}.
     * @return The successor {@link Node}.
     */
    public Node onFail() {
        return fail;
    }

    /**
     * Return the successor {@link Node}, invoked only when the current {@link Node} returns
     * {@link ActivityResult#SUSPEND}.
     * @return The successor {@link Node}.
     */
    public Node onSuspend() {
        return suspend;
    }

    /**
     * Return the successor {@link Node}, invoked only when the current {@link Node} returns
     * {@link ActivityResult#UNKNOWN}.
     * @return The successor {@link Node}.
     */
    public Node onCheck() {
        return check;
    }

    // CHECKSTYLE:OFF
    public enum NextStepType {
        SUCCESS, FAIL, SUSPEND, CHECK, CONDITION;
    }
    // CHECKSTYLE:ON
}

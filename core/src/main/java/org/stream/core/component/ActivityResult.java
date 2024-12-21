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

package org.stream.core.component;

/**
 * {@link Activity} act result.
 * @author guanxiong wei
 *
 */
public enum ActivityResult {

    /**
     * Success result.
     */
    SUCCESS {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.onSuccess();
        }
    },

    /**
     * Fail result.
     */
    FAIL {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.onFail();
        }
    },

    /**
     * Result unknown after the node is executed, sometimes extra effort needs to be taken to check if everything is okay.
     */
    UNKNOWN {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.onCheck();
        }
    },

    /**
     * Unknown result, maybe the current work flow should be suspended and tried again later.
     */
    SUSPEND {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.onSuspend();
        }
    },

    /**
     * Guiding the workflow engine to invoke another procedure, then go back to the next
     * succeed node.
     */
    INVOKE {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.onInvoke();
        }
    },

    /**
     * On condition result, giving users a way to define self specific strategy to determine next steps.
     */
    CONDITION {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.onCondition();
        }
    };

    public static ActivityResult condition(final int code) {
        CONDITION_CODE.set(code);
        return ActivityResult.CONDITION;
    }

    public static ActivityResult invoke(final String graph) {
        INVOKE_GRAPH.set(graph);
        return ActivityResult.INVOKE;
    }

    /**
     * Thread local storage to hold the latest activity result condition code.
     * Users can return {@link #CONDITION} via method {@link #condition(int)} and
     * specify the code.
     * The Workflow engine will determine the next step by the condition code
     * and graph definition file and clear the condition code immediately.
     */
    public static final ThreadLocal<Integer> CONDITION_CODE = new ThreadLocal<>();

    /**
     * Thread local storage to hold the child procedure graph name specified by the
     * previous activity. Workflow engine will clear the graph name immediately after it
     * retrieves the graph instance from the context.
     */
    public static final ThreadLocal<String> INVOKE_GRAPH = new ThreadLocal<>();

    // CHECKSTYLE:OFF
    public abstract <E> E accept(final Visitor<E> visitor);

    public interface Visitor<T> {

        /**
         * Visit the success step.
         * @return Processing result.
         */
        T onSuccess();

        /**
         * Visit the failure step.
         * @return Processing result.
         */
        T onFail();

        /**
         * Visit the suspend step.
         * @return Processing result.
         */
        T onSuspend();

        /**
         * Visit the check step.
         * @return Processing result.
         */
        T onCheck();

        /**
         * Visit the invoked step.
         * @return Processing result from the invoked child procedure.
         */
        T onInvoke();

        /**
         * Visit the user specific on a condition step.
         * @return Processing result.
         */
        T onCondition();
    }
    // CHECKSTYLE:ON
}

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
     * Result unknown after the node is executed, sometimes extra effort need to be taken to check if everything is okay.
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
     * specify the code. Workflow engine will determine the next step by the condition code
     * and graph definition file and clear the condition code immediately.
     */
    public static final ThreadLocal<Integer> CONDITION_CODE = new ThreadLocal<>();

    /**
     * Thread local storage to hold the child procedure graph name specified by the
     * previous activity. Workflow engine will clear the graph name immediately after it
     * retrieve the graph instance from the context.
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
         * Visit the invoke step.
         * @return Processing result from the invoked child procedure.
         */
        T onInvoke();

        /**
         * Visit the user specific on condition step.
         * @return Processing result.
         */
        T onCondition();
    }
    // CHECKSTYLE:ON
}

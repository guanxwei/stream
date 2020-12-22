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
     * On condition result, giving users a way to define self specific strategy to determine next steps.
     */
    CONDITION {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.onSuspend();
        }
    };

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
         * Visit the user specific on condition step.
         * @return Processing result.
         */
        T onCondition();
    }
    // CHECKSTYLE:ON
}

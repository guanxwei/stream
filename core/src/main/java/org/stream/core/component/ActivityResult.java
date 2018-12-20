package org.stream.core.component;

/**
 * {@link Activity} act result.
 * @author hzweiguanxiong
 *
 */
public enum ActivityResult {

    /**
     * Success result.
     */
    SUCCESS {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.success();
        }
    },

    /**
     * Fail result.
     */
    FAIL {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.fail();
        }
    },

    /**
     * Result unknown after the node is executed, sometimes extra effort need to be taken to check if everything is okay.
     */
    UNKNOWN {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.check();
        }
    },

    /**
     * Unknown result, maybe the current work flow should be suspended and tried again later.
     */
    SUSPEND {
        @Override
        public <E> E accept(final Visitor<E> visitor) {
            return visitor.suspend();
        }
    };

    // CHECKSTYLE:OFF
    public abstract <E> E accept(final Visitor<E> visitor);

    public interface Visitor<T> {

        /**
         * Visit the success step.
         * @return Processing result.
         */
        T success();

        /**
         * Visit the failure step.
         * @return Processing result.
         */
        T fail();

        /**
         * Visit the suspend step.
         * @return Processing result.
         */
        T suspend();

        /**
         * Visit the check step.
         * @return Processing result.
         */
        T check();
    }
    // CHECKSTYLE:ON
}

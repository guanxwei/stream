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
     * Unknown result, maybe the current thread should be suspened or not.
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
        T success();
        T fail();
        T suspend();
    }
    // CHECKSTYLE:ON
}

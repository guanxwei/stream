package org.stream.core.component;

public enum ActivityResult {

    SUCCESS {
        @Override
        public <E> E accept(Visitor<E> visitor) {
            return visitor.success();
        }
    },

    FAIL {
        @Override
        public <E> E accept(Visitor<E> visitor) {
            return visitor.fail();
        }
    },

    SUSPEND {
        @Override
        public <E> E accept(Visitor<E> visitor) {
            return visitor.suspend();
        }
    };

    public abstract <E> E accept(final Visitor<E> visitor);

    public interface Visitor<T> {
        T success();
        T fail();
        T suspend();
    }
}

package org.stream.extension.utils.actionable;

import lombok.extern.slf4j.Slf4j;

/**
 * Tellme class. Just tell this class what to do when the condition fulfilles.
 * @author guanxiongwei
 *
 */
@Slf4j
public final class Tellme {

    private Tellme() { }

    /**
     * Start a new state.
     * @param condition Condition.
     * @return New state.
     */
    public static State when(final boolean condition) {
        if (condition) {
            return new TrueState();
        }

        return new FalseState();
    }

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

    /**
     * State.
     * @author guanxiongwei
     *
     */
    public static abstract class State {

        /**
         * Function to be executed when this state fulfilles condition.
         * @param operation Action.
         */
        public abstract void then(final Operation operation);
    }

    /**
     * False state.
     * @author guanxiongwei
     *
     */
    public static class FalseState extends State {

        /**
         * {@inheritDoc}
         */
        @Override
        public void then(final Operation operation) {
            log.info("False condition, do nothing");
        }
    }

    /**
     * True state.
     * @author guanxiongwei
     *
     */
    public static class TrueState extends State {

        /**
         * {@inheritDoc}
         */
        @Override
        public void then(final Operation operation) {
            operation.operate();
        }
    }

    /**
     * Operation.
     * @author guanxiongwei
     *
     */
    public interface Operation {

        /**
         * Do something.
         */
        void operate();
    }
}

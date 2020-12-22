package org.stream.core.exception;

/**
 * Work-flow execution exception, may be thrown during work-flow execution process.
 * @author guanxiong wei
 *
 */
public class WorkFlowExecutionExeception extends RuntimeException {

    private static final long serialVersionUID = -9138588163767012469L;

    // CHECKSTYLE:OFF
    public WorkFlowExecutionExeception(final String message) {
        super(message);
    }

    public WorkFlowExecutionExeception(final String message, final Throwable t) {
        super(message, t);
    }

    public WorkFlowExecutionExeception(final Throwable t) {
        super(t);
    }
    // CHECKSTYLE:ON
}

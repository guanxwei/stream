package org.stream.core.exception;

public class WorkFlowExecutionExeception extends RuntimeException {

    private static final long serialVersionUID = -9138588163767012469L;

    public WorkFlowExecutionExeception(String message) {
        super(message);
    }

    public WorkFlowExecutionExeception(String message, Throwable t) {
        super(message, t);
    }

    public WorkFlowExecutionExeception(Throwable t) {
        super(t);
    }
}

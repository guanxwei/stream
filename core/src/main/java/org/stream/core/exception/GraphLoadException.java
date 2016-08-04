package org.stream.core.exception;

public class GraphLoadException extends StreamException {

    private static final long serialVersionUID = 6245569938672328171L;

    public GraphLoadException(String message) {
        super(message);
    }

    public GraphLoadException(String message, Throwable t) {
        super(message, t);
    }

    public GraphLoadException(Throwable t) {
        super(t);
    }
}

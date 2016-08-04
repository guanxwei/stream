package org.stream.core.exception;

public class StreamException extends Exception {

    private static final long serialVersionUID = 990113735174849552L;

    public StreamException(String message) {
        super(message);
    }

    public StreamException(String message, Throwable t) {
        super(message, t);
    }

    public StreamException(Throwable t) {
        super(t);
    }

}

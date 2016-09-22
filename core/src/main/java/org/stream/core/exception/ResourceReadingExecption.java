package org.stream.core.exception;

public class ResourceReadingExecption extends Exception {

    private static final long serialVersionUID = -2560301499253809003L;

    public ResourceReadingExecption(String message) {
        super(message);
    }

    public ResourceReadingExecption(String message, Throwable t) {
        super(message, t);
    }

    public ResourceReadingExecption(Throwable t) {
        super(t);
    }
}

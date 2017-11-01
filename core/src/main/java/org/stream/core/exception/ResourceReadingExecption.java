package org.stream.core.exception;

/**
 * Resource reading exception.
 * @author hzweiguanxiong
 *
 */
public class ResourceReadingExecption extends Exception {

    private static final long serialVersionUID = -2560301499253809003L;

    // CHECKSTYLE:OFF
    public ResourceReadingExecption(final String message) {
        super(message);
    }

    public ResourceReadingExecption(final String message, final Throwable t) {
        super(message, t);
    }

    public ResourceReadingExecption(final Throwable t) {
        super(t);
    }
    // CHECKSTYLE:ON
}

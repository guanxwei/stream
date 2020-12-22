package org.stream.core.exception;

/**
 * Stream work-flow framework exception, may be thrown in any place of the work-flow context.
 * @author guanxiong wei
 *
 */
public class StreamException extends Exception {

    private static final long serialVersionUID = 990113735174849552L;

    // CHECKSTYLE:OFF
    public StreamException(final String message) {
        super(message);
    }

    public StreamException(final String message, final Throwable t) {
        super(message, t);
    }

    public StreamException(final Throwable t) {
        super(t);
    }
    // CHECKSTYLE:ON
}

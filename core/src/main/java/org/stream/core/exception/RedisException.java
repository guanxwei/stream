package org.stream.core.exception;

/**
 * Exception thrown during operate Redis clients.
 *
 */
public class RedisException extends RuntimeException {

    private static final long serialVersionUID = -423801385627332575L;

    // CHECKSTYLE:OFF
    public RedisException(final String message) {
        super(message);
    }

    public RedisException(final String message, final Throwable t) {
        super(message, t);
    }

    public RedisException(final Throwable t) {
        super(t);
    }
    // CHECKSTYLE:ON
}

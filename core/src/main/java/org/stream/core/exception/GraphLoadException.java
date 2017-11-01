package org.stream.core.exception;

/**
 * Graph load exception.
 *
 */
public class GraphLoadException extends StreamException {

    private static final long serialVersionUID = 6245569938672328171L;

    // CHECKSTYLE:OFF
    public GraphLoadException(final String message) {
        super(message);
    }

    public GraphLoadException(final String message, final Throwable t) {
        super(message, t);
    }

    public GraphLoadException(final Throwable t) {
        super(t);
    }
    // CHECKSTYLE:ON
}

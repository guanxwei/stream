package org.stream.core.exception;

/** 
 * A generic exception that can be thrown when encode or decode json strings.
*/
public class JasonException extends RuntimeException {
    
    /**
     * Exposed constructor.
     * @param message Error message.
     * @param t The cause the throwable is raised.
     */
    public JasonException(String message, Throwable t) {
        super(message, t);
    }
}

package io.github.mbdo.factoryscada.socket.exception;

/**
 * Exception class for protocol-specific errors.
 */
public class ProtocolException extends Exception {
    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.bank.statementprocessor.exception;

/**
 * This should map to HTTP 500 Internal Server Error.
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

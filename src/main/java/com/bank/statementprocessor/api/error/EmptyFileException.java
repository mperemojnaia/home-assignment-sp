package com.bank.statementprocessor.api.error;

/**
 * Exception thrown when an uploaded file is empty.
 * This should map to HTTP 400 Bad Request.
 */
public class EmptyFileException extends RuntimeException {
    
    public EmptyFileException(String message) {
        super(message);
    }
}

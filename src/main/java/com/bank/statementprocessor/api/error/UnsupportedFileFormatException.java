package com.bank.statementprocessor.api.error;

/**
 * This should map to HTTP 415 Unsupported Media Type.
 */
public class UnsupportedFileFormatException extends RuntimeException {
    
    public UnsupportedFileFormatException(String message) {
        super(message);
    }
    
    public UnsupportedFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

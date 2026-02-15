package com.bank.statementprocessor.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Exception thrown when parsing transaction files fails.
 * Provides detailed context about the parsing error, including line number and field name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ParseException extends RuntimeException {

    private int lineNumber;

    private String fieldName;

    private String message;

    public ParseException(String message) {
        super(message);
        this.message = message;
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}

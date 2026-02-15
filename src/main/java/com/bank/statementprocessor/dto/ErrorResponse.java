package com.bank.statementprocessor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Matches the OpenAPI schema for ErrorResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Error message describing what went wrong.
     */
    private String error;
    
    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;
}

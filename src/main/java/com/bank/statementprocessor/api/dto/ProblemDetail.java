package com.bank.statementprocessor.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "RFC 7807 Problem Details for structured error responses")
public record ProblemDetail(
    
    @Schema(description = "HTTP status code", example = "400")
    int status,
    
    @Schema(description = "Error type URI", example = "about:blank")
    String type,
    
    @Schema(description = "Short, human-readable summary", example = "Empty File")
    String title,
    
    @Schema(description = "Human-readable explanation", example = "The uploaded file is empty")
    String detail,
    
    @Schema(description = "URI reference to the specific occurrence", example = "/api/v1/transaction-records/validate")
    String instance,
    
    @Schema(description = "Timestamp when the error occurred")
    Instant timestamp,
    
    @Schema(description = "Correlation ID for request tracking")
    String correlationId,
    
    @Schema(description = "Stable error code for client handling", example = "EMPTY_FILE")
    String errorCode
) {
}

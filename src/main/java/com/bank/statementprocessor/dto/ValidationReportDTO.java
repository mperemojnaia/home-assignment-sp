package com.bank.statementprocessor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Provides consistent API response structure with correlation ID for traceability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Validation report containing results of transaction validation")
public class ValidationReportDTO {
    
    /**
     * Whether all records passed validation.
     */
    @Schema(description = "Indicates if all records passed validation", example = "true")
    private boolean valid;
    
    /**
     * Total number of records processed.
     */
    @Schema(description = "Total number of transaction records processed", example = "10")
    private int totalRecords;
    
    /**
     * Number of records that failed validation.
     */
    @Schema(description = "Number of records that failed validation", example = "2")
    private int failedRecords;
    
    /**
     * List of validation failures.
     */
    @Schema(description = "Detailed list of validation failures")
    private List<ValidationFailureDTO> failures;
    
    /**
     * Correlation ID for request tracking.
     * Matches the correlationId in error responses for consistent traceability.
     */
    @Schema(description = "Correlation ID for request tracking", example = "550e8400-e29b-41d4-a716-446655440000")
    private String correlationId;
}

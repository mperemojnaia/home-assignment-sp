package com.bank.statementprocessor.dto;

import com.bank.statementprocessor.domain.ValidationFailure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Data Transfer Object for validation failure details.
 * Matches the OpenAPI schema for ValidationFailure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationFailureDTO {
    
    /**
     * Transaction reference number.
     */
    private Long reference;
    
    /**
     * Transaction description.
     */
    private String description;
    
    /**
     * List of validation failure reasons.
     */
    private Set<ValidationFailure.FailureReason> reasons;
}

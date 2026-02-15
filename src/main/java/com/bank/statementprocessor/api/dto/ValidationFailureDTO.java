package com.bank.statementprocessor.api.dto;

import com.bank.statementprocessor.core.model.ValidationFailure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
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

package com.bank.statementprocessor.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Represents a validation failure for a transaction record.
 * 
 * Contains the transaction reference, description, and the set of reasons
 * why the transaction failed validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationFailure {
    
    /**
     * Transaction reference number that failed validation
     */
    private Long reference;
    
    /**
     * Description of the transaction that failed validation
     */
    private String description;
    
    /**
     * Set of reasons why the transaction failed validation.
     * A transaction can fail for multiple reasons (e.g., both duplicate reference and incorrect balance).
     */
    private Set<FailureReason> reasons;
    
    /**
     * Enumeration of possible validation failure reasons.
     */
    public enum FailureReason {
        /**
         * The transaction reference appears more than once in the file
         */
        DUPLICATE_REFERENCE,
        
        /**
         * The end balance does not match the calculation: startBalance + mutation = endBalance
         */
        INCORRECT_END_BALANCE
    }
}

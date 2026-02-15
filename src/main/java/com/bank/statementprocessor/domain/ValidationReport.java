package com.bank.statementprocessor.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the result of validating a set of transaction records.
 * 
 * Contains information about whether all records passed validation,
 * the list of validation failures, and counts of total and failed records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReport {
    
    /**
     * Indicates whether all transaction records passed validation.
     * True if no validation failures occurred, false otherwise.
     */
    private boolean valid;
    
    /**
     * List of validation failures for transaction records that did not pass validation.
     * Empty list if all records are valid.
     */
    private List<ValidationFailure> failures;
    
    /**
     * Total number of transaction records processed during validation.
     */
    private int totalRecords;
    
    /**
     * Number of transaction records that failed validation.
     * Should equal the size of the failures list.
     */
    private int failedRecords;
}

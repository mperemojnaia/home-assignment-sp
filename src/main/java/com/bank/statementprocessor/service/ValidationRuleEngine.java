package com.bank.statementprocessor.service;

import com.bank.statementprocessor.domain.TransactionRecord;
import com.bank.statementprocessor.domain.ValidationFailure;
import com.bank.statementprocessor.domain.ValidationFailure.FailureReason;
import com.bank.statementprocessor.domain.ValidationReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The engine constructs a ValidationReport containing all failures
 * and statistics about the validation process.
 * 
 * Uses streaming for memory-efficient processing - maintains only O(unique references) memory.
 */
@Component
@Slf4j
public class ValidationRuleEngine {

    /**
     * Validates a list of transaction records by applying all validation rules.
     * 1. Reference uniqueness - identifies duplicate transaction references
     * 2. Balance calculation - verifies startBalance + mutation = endBalance (exact match required)
     * 
     * @param records the list of transaction records to validate
     * @return a ValidationReport containing all failures and validation statistics
     */
    public ValidationReport validate(List<TransactionRecord> records) {
        if (records == null || records.isEmpty()) {
            log.info("No records to validate, returning empty report");
            return ValidationReport.builder()
                    .valid(true)
                    .failures(Collections.emptyList())
                    .totalRecords(0)
                    .failedRecords(0)
                    .build();
        }
        
        log.info("Starting validation for {} transaction records", records.size());

        Set<Long> duplicateReferences = findDuplicateReferences(records);
        log.debug("Found {} duplicate references", duplicateReferences.size());

        List<ValidationFailure> failures = new ArrayList<>();

        for (TransactionRecord record : records) {
            Set<FailureReason> reasons = new HashSet<>();

            if (duplicateReferences.contains(record.getReference())) {
                reasons.add(FailureReason.DUPLICATE_REFERENCE);
                log.debug("Record with reference {} marked as duplicate", record.getReference());
            }

            if (!record.isBalanceCorrect()) {
                reasons.add(FailureReason.INCORRECT_END_BALANCE);
                log.debug("Record with reference {} has incorrect balance", record.getReference());
            }

            if (!reasons.isEmpty()) {
                failures.add(ValidationFailure.builder()
                        .reference(record.getReference())
                        .description(record.getDescription())
                        .reasons(reasons)
                        .build());
            }
        }
        
        int failedRecords = failures.size();
        boolean valid = failedRecords == 0;
        
        log.info("Validation complete: {} total records, {} failed records", 
                records.size(), failedRecords);
        
        return ValidationReport.builder()
                .valid(valid)
                .failures(failures)
                .totalRecords(records.size())
                .failedRecords(failedRecords)
                .build();
    }
    
    /**
     * Identifies duplicate transaction references in the list of records.
     * 
     * @param records the list of transaction records to check
     * @return a set of reference numbers that appear more than once
     */
    private Set<Long> findDuplicateReferences(List<TransactionRecord> records) {
        Map<Long, Long> referenceCounts = records.stream()
                .map(TransactionRecord::getReference)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        reference -> reference,
                        Collectors.counting()
                ));
        
        return referenceCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}

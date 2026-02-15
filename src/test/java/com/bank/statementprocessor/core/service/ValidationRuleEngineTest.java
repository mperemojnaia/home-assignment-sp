package com.bank.statementprocessor.core.service;

import com.bank.statementprocessor.core.model.TransactionRecord;
import com.bank.statementprocessor.core.model.ValidationFailure;
import com.bank.statementprocessor.core.model.ValidationFailure.FailureReason;
import com.bank.statementprocessor.core.model.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Unit tests for ValidationRuleEngine.
 * 
 * Tests cover:
 * - Validation of records with correct balances
 * - Identification of records with incorrect balances
 * - Identification of duplicate references (including sample data case with reference 112806)
 * - Handling of records with both duplicate reference and incorrect balance
 * - Handling of empty record lists
 * - Handling of lists with all valid records
 */
class ValidationRuleEngineTest {
    
    private ValidationRuleEngine validationRuleEngine;
    
    @BeforeEach
    void setUp() {
        validationRuleEngine = new ValidationRuleEngine();
    }
    
    @Test
    void testValidateEmptyList() {
        // Given
        List<TransactionRecord> records = Collections.emptyList();
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then
        assertThat(report.isValid()).isTrue();
        assertThat(report.getFailures()).isEmpty();
        assertThat(report.getTotalRecords()).isEqualTo(0);
        assertThat(report.getFailedRecords()).isEqualTo(0);
    }
    
    @Test
    void testValidateNullList() {
        // Given
        List<TransactionRecord> records = null;
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then
        assertThat(report.isValid()).isTrue();
        assertThat(report.getFailures()).isEmpty();
        assertThat(report.getTotalRecords()).isEqualTo(0);
        assertThat(report.getFailedRecords()).isEqualTo(0);
    }
    
    @Test
    void testValidateAllValidRecords() {
        // Given
        List<TransactionRecord> records = Arrays.asList(
                TransactionRecord.builder()
                        .reference(194261L)
                        .accountNumber("NL91RABO0315273637")
                        .description("Clothes from Jan Bakker")
                        .startBalance(new BigDecimal("21.6"))
                        .mutation(new BigDecimal("-41.83"))
                        .endBalance(new BigDecimal("-20.23"))
                        .build(),
                TransactionRecord.builder()
                        .reference(183049L)
                        .accountNumber("NL69ABNA0433647324")
                        .description("Clothes for Willem Dekker")
                        .startBalance(new BigDecimal("86.66"))
                        .mutation(new BigDecimal("44.5"))
                        .endBalance(new BigDecimal("131.16"))
                        .build()
        );
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then
        assertThat(report.isValid()).isTrue();
        assertThat(report.getFailures()).isEmpty();
        assertThat(report.getTotalRecords()).isEqualTo(2);
        assertThat(report.getFailedRecords()).isEqualTo(0);
    }
    
    @Test
    void testValidateRecordWithIncorrectBalance() {
        // Given
        List<TransactionRecord> records = Arrays.asList(
                TransactionRecord.builder()
                        .reference(194261L)
                        .accountNumber("NL91RABO0315273637")
                        .description("Clothes from Jan Bakker")
                        .startBalance(new BigDecimal("21.6"))
                        .mutation(new BigDecimal("-41.83"))
                        .endBalance(new BigDecimal("-20.00")) // Incorrect: should be -20.23
                        .build()
        );
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then
        assertThat(report.isValid()).isFalse();
        assertThat(report.getFailures()).hasSize(1);
        assertThat(report.getTotalRecords()).isEqualTo(1);
        assertThat(report.getFailedRecords()).isEqualTo(1);
        
        ValidationFailure failure = report.getFailures().get(0);
        assertThat(failure.getReference()).isEqualTo(194261L);
        assertThat(failure.getDescription()).isEqualTo("Clothes from Jan Bakker");
        assertThat(failure.getReasons()).containsExactly(FailureReason.INCORRECT_END_BALANCE);
    }
    
    @Test
    void testValidateDuplicateReferences() {
        // Given - Sample data case with reference 112806 appearing 3 times
        List<TransactionRecord> records = Arrays.asList(
                TransactionRecord.builder()
                        .reference(112806L)
                        .accountNumber("NL27SNSB0917829871")
                        .description("Clothes from Richard de Vries")
                        .startBalance(new BigDecimal("91.23"))
                        .mutation(new BigDecimal("15.57"))
                        .endBalance(new BigDecimal("106.8"))
                        .build(),
                TransactionRecord.builder()
                        .reference(112806L)
                        .accountNumber("NL69ABNA0433647324")
                        .description("Clothes for Willem Dekker")
                        .startBalance(new BigDecimal("86.66"))
                        .mutation(new BigDecimal("44.5"))
                        .endBalance(new BigDecimal("131.16"))
                        .build(),
                TransactionRecord.builder()
                        .reference(112806L)
                        .accountNumber("NL43AEGO0773393871")
                        .description("Subscription from Peter de Vries")
                        .startBalance(new BigDecimal("66.72"))
                        .mutation(new BigDecimal("45.9"))
                        .endBalance(new BigDecimal("112.62"))
                        .build()
        );
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then
        assertThat(report.isValid()).isFalse();
        assertThat(report.getTotalRecords()).isEqualTo(3);
        assertThat(report.getFailedRecords()).isEqualTo(3);
        
        // All three records should be marked as duplicate - contract-oriented assertion
        assertThat(report.getFailures())
                .hasSize(3)
                .extracting(ValidationFailure::getReference)
                .containsOnly(112806L);
        
        assertThat(report.getFailures())
                .flatExtracting(ValidationFailure::getReasons)
                .containsOnly(FailureReason.DUPLICATE_REFERENCE);
    }
    
    @Test
    void testValidateDuplicateReferencesWithTwoOccurrences() {
        // Given - Two records with the same reference
        List<TransactionRecord> records = Arrays.asList(
                TransactionRecord.builder()
                        .reference(100001L)
                        .accountNumber("NL27SNSB0917829871")
                        .description("First transaction")
                        .startBalance(new BigDecimal("100.00"))
                        .mutation(new BigDecimal("10.00"))
                        .endBalance(new BigDecimal("110.00"))
                        .build(),
                TransactionRecord.builder()
                        .reference(100001L)
                        .accountNumber("NL69ABNA0433647324")
                        .description("Second transaction")
                        .startBalance(new BigDecimal("200.00"))
                        .mutation(new BigDecimal("20.00"))
                        .endBalance(new BigDecimal("220.00"))
                        .build()
        );
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then
        assertThat(report.isValid()).isFalse();
        assertThat(report.getTotalRecords()).isEqualTo(2);
        assertThat(report.getFailedRecords()).isEqualTo(2);
        
        // Both records should be marked as duplicate - contract-oriented assertion
        assertThat(report.getFailures())
                .hasSize(2)
                .extracting(ValidationFailure::getReference)
                .containsOnly(100001L);
        
        assertThat(report.getFailures())
                .flatExtracting(ValidationFailure::getReasons)
                .containsOnly(FailureReason.DUPLICATE_REFERENCE);
    }
    
    @Test
    void testValidateRecordWithBothDuplicateReferenceAndIncorrectBalance() {
        // Given - Two records with same reference, one has incorrect balance
        List<TransactionRecord> records = Arrays.asList(
                TransactionRecord.builder()
                        .reference(100001L)
                        .accountNumber("NL27SNSB0917829871")
                        .description("First transaction")
                        .startBalance(new BigDecimal("100.00"))
                        .mutation(new BigDecimal("10.00"))
                        .endBalance(new BigDecimal("110.00")) // Correct balance
                        .build(),
                TransactionRecord.builder()
                        .reference(100001L)
                        .accountNumber("NL69ABNA0433647324")
                        .description("Second transaction")
                        .startBalance(new BigDecimal("200.00"))
                        .mutation(new BigDecimal("20.00"))
                        .endBalance(new BigDecimal("200.00")) // Incorrect: should be 220.00
                        .build()
        );
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then
        assertThat(report.isValid()).isFalse();
        assertThat(report.getTotalRecords()).isEqualTo(2);
        assertThat(report.getFailedRecords()).isEqualTo(2);
        
        // Contract-oriented: assert which failure reasons appear for each record
        assertThat(report.getFailures())
                .hasSize(2)
                .extracting(ValidationFailure::getDescription, ValidationFailure::getReasons)
                .containsExactlyInAnyOrder(
                        tuple("First transaction", Set.of(FailureReason.DUPLICATE_REFERENCE)),
                        tuple("Second transaction", Set.of(FailureReason.DUPLICATE_REFERENCE, FailureReason.INCORRECT_END_BALANCE))
                );
    }
    
    @Test
    void testValidateMixedValidAndInvalidRecords() {
        // Given - Mix of valid records, duplicate references, and incorrect balances
        List<TransactionRecord> records = Arrays.asList(
                // Valid record
                TransactionRecord.builder()
                        .reference(194261L)
                        .accountNumber("NL91RABO0315273637")
                        .description("Valid transaction")
                        .startBalance(new BigDecimal("21.6"))
                        .mutation(new BigDecimal("-41.83"))
                        .endBalance(new BigDecimal("-20.23"))
                        .build(),
                // Duplicate reference (first occurrence)
                TransactionRecord.builder()
                        .reference(112806L)
                        .accountNumber("NL27SNSB0917829871")
                        .description("Duplicate 1")
                        .startBalance(new BigDecimal("91.23"))
                        .mutation(new BigDecimal("15.57"))
                        .endBalance(new BigDecimal("106.8"))
                        .build(),
                // Duplicate reference (second occurrence)
                TransactionRecord.builder()
                        .reference(112806L)
                        .accountNumber("NL69ABNA0433647324")
                        .description("Duplicate 2")
                        .startBalance(new BigDecimal("86.66"))
                        .mutation(new BigDecimal("44.5"))
                        .endBalance(new BigDecimal("131.16"))
                        .build(),
                // Incorrect balance
                TransactionRecord.builder()
                        .reference(183049L)
                        .accountNumber("NL43AEGO0773393871")
                        .description("Incorrect balance")
                        .startBalance(new BigDecimal("66.72"))
                        .mutation(new BigDecimal("45.9"))
                        .endBalance(new BigDecimal("100.00")) // Incorrect: should be 112.62
                        .build()
        );
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then
        assertThat(report.isValid()).isFalse();
        assertThat(report.getTotalRecords()).isEqualTo(4);
        assertThat(report.getFailedRecords()).isEqualTo(3);
        
        // Contract-oriented: assert which references failed and why
        assertThat(report.getFailures())
                .hasSize(3)
                .extracting(ValidationFailure::getReference, ValidationFailure::getDescription)
                .containsExactlyInAnyOrder(
                        tuple(112806L, "Duplicate 1"),
                        tuple(112806L, "Duplicate 2"),
                        tuple(183049L, "Incorrect balance")
                );
        
        // Assert which failure reasons appear
        assertThat(report.getFailures())
                .filteredOn(f -> f.getReference().equals(112806L))
                .flatExtracting(ValidationFailure::getReasons)
                .containsOnly(FailureReason.DUPLICATE_REFERENCE);
        
        assertThat(report.getFailures())
                .filteredOn(f -> f.getReference().equals(183049L))
                .flatExtracting(ValidationFailure::getReasons)
                .containsOnly(FailureReason.INCORRECT_END_BALANCE);
    }
    
    @Test
    void testValidateRecordWithNullReference() {
        // Given - Record with null reference
        List<TransactionRecord> records = Arrays.asList(
                TransactionRecord.builder()
                        .reference(null)
                        .accountNumber("NL91RABO0315273637")
                        .description("Transaction with null reference")
                        .startBalance(new BigDecimal("21.6"))
                        .mutation(new BigDecimal("-41.83"))
                        .endBalance(new BigDecimal("-20.23"))
                        .build()
        );
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then - Should not crash, record should pass (null references are filtered out from duplicate check)
        assertThat(report.isValid()).isTrue();
        assertThat(report.getFailures()).isEmpty();
        assertThat(report.getTotalRecords()).isEqualTo(1);
        assertThat(report.getFailedRecords()).isEqualTo(0);
    }
    
    @Test
    void testValidateRecordWithNullBalanceFields() {
        // Given - Record with null balance fields (will fail isBalanceCorrect())
        List<TransactionRecord> records = Arrays.asList(
                TransactionRecord.builder()
                        .reference(194261L)
                        .accountNumber("NL91RABO0315273637")
                        .description("Transaction with null balance")
                        .startBalance(null)
                        .mutation(new BigDecimal("-41.83"))
                        .endBalance(new BigDecimal("-20.23"))
                        .build()
        );
        
        // When
        ValidationReport report = validationRuleEngine.validate(records);
        
        // Then - Should fail balance validation
        assertThat(report.isValid()).isFalse();
        assertThat(report.getFailures()).hasSize(1);
        assertThat(report.getFailures().get(0).getReasons())
                .containsExactly(FailureReason.INCORRECT_END_BALANCE);
    }
}

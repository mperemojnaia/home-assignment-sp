package com.bank.statementprocessor.mapper;

import com.bank.statementprocessor.domain.ValidationFailure;
import com.bank.statementprocessor.domain.ValidationReport;
import com.bank.statementprocessor.dto.ValidationReportDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ValidationReportMapper.
 * Tests null-safety approach: required objects throw NPE, collections treated as empty.
 */
class ValidationReportMapperTest {
    
    private final ValidationReportMapper mapper = new ValidationReportMapper();
    
    @Test
    void testToDTO_WithValidReport() {
        // Given
        ValidationFailure failure1 = ValidationFailure.builder()
                .reference(112806L)
                .description("Test transaction 1")
                .reasons(Set.of(ValidationFailure.FailureReason.DUPLICATE_REFERENCE))
                .build();
        
        ValidationFailure failure2 = ValidationFailure.builder()
                .reference(194261L)
                .description("Test transaction 2")
                .reasons(Set.of(ValidationFailure.FailureReason.INCORRECT_END_BALANCE))
                .build();
        
        ValidationReport report = ValidationReport.builder()
                .valid(false)
                .totalRecords(10)
                .failedRecords(2)
                .failures(List.of(failure1, failure2))
                .build();
        
        // When
        ValidationReportDTO dto = mapper.toDTO(report);
        
        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.isValid()).isFalse();
        assertThat(dto.getTotalRecords()).isEqualTo(10);
        assertThat(dto.getFailedRecords()).isEqualTo(2);
        assertThat(dto.getFailures()).hasSize(2);
        
        assertThat(dto.getFailures().get(0).getReference()).isEqualTo(112806L);
        assertThat(dto.getFailures().get(0).getDescription()).isEqualTo("Test transaction 1");
        assertThat(dto.getFailures().get(0).getReasons())
                .contains(ValidationFailure.FailureReason.DUPLICATE_REFERENCE);
        
        assertThat(dto.getFailures().get(1).getReference()).isEqualTo(194261L);
        assertThat(dto.getFailures().get(1).getDescription()).isEqualTo("Test transaction 2");
        assertThat(dto.getFailures().get(1).getReasons())
                .contains(ValidationFailure.FailureReason.INCORRECT_END_BALANCE);
    }
    
    @Test
    void testToDTO_WithValidReportNoFailures() {
        // Given
        ValidationReport report = ValidationReport.builder()
                .valid(true)
                .totalRecords(5)
                .failedRecords(0)
                .failures(List.of())
                .build();
        
        // When
        ValidationReportDTO dto = mapper.toDTO(report);
        
        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.isValid()).isTrue();
        assertThat(dto.getTotalRecords()).isEqualTo(5);
        assertThat(dto.getFailedRecords()).isEqualTo(0);
        assertThat(dto.getFailures()).isEmpty();
    }
    
    @Test
    void testToDTO_WithNullReport_ThrowsNullPointerException() {
        // When/Then - null report is a programmer error
        assertThatThrownBy(() -> mapper.toDTO(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ValidationReport must not be null");
    }
    
    @Test
    void testToDTO_WithNullFailuresList_TreatsAsEmpty() {
        // Given - defensive: treat null failures as empty
        ValidationReport report = ValidationReport.builder()
                .valid(true)
                .totalRecords(5)
                .failedRecords(0)
                .failures(null)  // null list
                .build();
        
        // When
        ValidationReportDTO dto = mapper.toDTO(report);
        
        // Then - should not throw, treats null as empty
        assertThat(dto).isNotNull();
        assertThat(dto.isValid()).isTrue();
        assertThat(dto.getFailures()).isEmpty();
    }
    
    @Test
    void testToDTO_WithNullReasonsSet_TreatsAsEmpty() {
        // Given - defensive: treat null reasons as empty
        ValidationFailure failure = ValidationFailure.builder()
                .reference(112806L)
                .description("Test transaction")
                .reasons(null)  // null set
                .build();
        
        ValidationReport report = ValidationReport.builder()
                .valid(false)
                .totalRecords(1)
                .failedRecords(1)
                .failures(List.of(failure))
                .build();
        
        // When
        ValidationReportDTO dto = mapper.toDTO(report);
        
        // Then - should not throw, treats null as empty
        assertThat(dto).isNotNull();
        assertThat(dto.getFailures()).hasSize(1);
        assertThat(dto.getFailures().get(0).getReasons()).isEmpty();
    }
    
    @Test
    void testToDTO_WithNullElementsInFailuresList_FiltersThemOut() {
        // Given - defensive: filter out null elements from list
        ValidationFailure validFailure = ValidationFailure.builder()
                .reference(112806L)
                .description("Valid failure")
                .reasons(Set.of(ValidationFailure.FailureReason.DUPLICATE_REFERENCE))
                .build();
        
        List<ValidationFailure> failuresWithNulls = new java.util.ArrayList<>();
        failuresWithNulls.add(validFailure);
        failuresWithNulls.add(null);  // null element
        failuresWithNulls.add(null);  // another null element
        
        ValidationReport report = ValidationReport.builder()
                .valid(false)
                .totalRecords(3)
                .failedRecords(3)
                .failures(failuresWithNulls)
                .build();
        
        // When
        ValidationReportDTO dto = mapper.toDTO(report);
        
        // Then - null elements filtered out, only valid failure remains
        assertThat(dto).isNotNull();
        assertThat(dto.getFailures()).hasSize(1);
        assertThat(dto.getFailures().get(0).getReference()).isEqualTo(112806L);
        assertThat(dto.getFailures().get(0).getDescription()).isEqualTo("Valid failure");
    }
    
    @Test
    void testToDTO_WithNullElementsInReasonsSet_FiltersThemOut() {
        // Given - defensive: filter out null elements from set
        Set<ValidationFailure.FailureReason> reasonsWithNulls = new java.util.HashSet<>();
        reasonsWithNulls.add(ValidationFailure.FailureReason.DUPLICATE_REFERENCE);
        reasonsWithNulls.add(null);  // null element
        
        ValidationFailure failure = ValidationFailure.builder()
                .reference(112806L)
                .description("Test transaction")
                .reasons(reasonsWithNulls)
                .build();
        
        ValidationReport report = ValidationReport.builder()
                .valid(false)
                .totalRecords(1)
                .failedRecords(1)
                .failures(List.of(failure))
                .build();
        
        // When
        ValidationReportDTO dto = mapper.toDTO(report);
        
        // Then - null elements filtered out, only valid reason remains
        assertThat(dto).isNotNull();
        assertThat(dto.getFailures()).hasSize(1);
        assertThat(dto.getFailures().get(0).getReasons()).hasSize(1);
        assertThat(dto.getFailures().get(0).getReasons())
                .contains(ValidationFailure.FailureReason.DUPLICATE_REFERENCE);
    }
    
    @Test
    void testToDTO_WithMultipleFailureReasons() {
        // Given
        ValidationFailure failure = ValidationFailure.builder()
                .reference(112806L)
                .description("Test transaction")
                .reasons(Set.of(
                        ValidationFailure.FailureReason.DUPLICATE_REFERENCE,
                        ValidationFailure.FailureReason.INCORRECT_END_BALANCE
                ))
                .build();
        
        ValidationReport report = ValidationReport.builder()
                .valid(false)
                .totalRecords(1)
                .failedRecords(1)
                .failures(List.of(failure))
                .build();
        
        // When
        ValidationReportDTO dto = mapper.toDTO(report);
        
        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getFailures()).hasSize(1);
        assertThat(dto.getFailures().get(0).getReasons()).hasSize(2);
        assertThat(dto.getFailures().get(0).getReasons())
                .contains(
                        ValidationFailure.FailureReason.DUPLICATE_REFERENCE,
                        ValidationFailure.FailureReason.INCORRECT_END_BALANCE
                );
    }
}

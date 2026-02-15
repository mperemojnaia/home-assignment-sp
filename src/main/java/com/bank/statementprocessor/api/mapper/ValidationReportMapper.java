package com.bank.statementprocessor.api.mapper;

import com.bank.statementprocessor.core.model.ValidationFailure;
import com.bank.statementprocessor.core.model.ValidationReport;
import com.bank.statementprocessor.api.dto.ValidationFailureDTO;
import com.bank.statementprocessor.api.dto.ValidationReportDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ValidationReportMapper {
    
    /**
     * Maps ValidationReport domain model to ValidationReportDTO.
     *
     * @param report the domain model (must not be null)
     * @return the DTO for API response
     * @throws NullPointerException if report is null
     */
    public ValidationReportDTO toDTO(ValidationReport report) {
        Objects.requireNonNull(report, "ValidationReport must not be null");
        
        List<ValidationFailure> failures = report.getFailures() != null
                ? report.getFailures() 
                : List.of();
        
        return ValidationReportDTO.builder()
                .valid(report.isValid())
                .totalRecords(report.getTotalRecords())
                .failedRecords(report.getFailedRecords())
                .failures(failures.stream()
                        .filter(Objects::nonNull)  // Filter out null elements
                        .map(this::toFailureDTO)
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * Maps ValidationFailure domain model to ValidationFailureDTO.
     *
     * @param failure the domain model (must not be null)
     * @return the DTO for API response
     * @throws NullPointerException if failure is null
     */
    private ValidationFailureDTO toFailureDTO(ValidationFailure failure) {
        Objects.requireNonNull(failure, "ValidationFailure must not be null");
        
        Set<ValidationFailure.FailureReason> reasons = failure.getReasons() != null
                ? failure.getReasons()
                : Set.of();
        
        Set<ValidationFailure.FailureReason> nonNullReasons = reasons.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        return ValidationFailureDTO.builder()
                .reference(failure.getReference())
                .description(failure.getDescription())
                .reasons(nonNullReasons)
                .build();
    }
}

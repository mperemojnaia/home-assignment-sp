package com.bank.statementprocessor.api.controller;

import com.bank.statementprocessor.core.model.ValidationReport;
import com.bank.statementprocessor.api.dto.ValidationReportDTO;
import com.bank.statementprocessor.api.error.EmptyFileException;
import com.bank.statementprocessor.api.mapper.ValidationReportMapper;
import com.bank.statementprocessor.core.service.TransactionRecordValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for transaction record validation endpoints.
 */
@RestController
@RequestMapping("/api/v1/transaction-records")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Record Validation", description = "API for validating bank transaction records from CSV or JSON files")
public class TransactionRecordController {
    
    private final TransactionRecordValidationService validationService;
    private final ValidationReportMapper validationReportMapper;
    
    /**
     * Validates a transaction record file.
     *
     * @param file the uploaded CSV or JSON file containing transaction records
     * @return ValidationReportDTO containing validation results
     */
    @Operation(
            summary = "Validate transaction record file",
            description = "Upload a CSV or JSON file containing transaction records for validation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Validation completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationReportDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - file missing or malformed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.bank.statementprocessor.api.dto.ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "File too large",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.bank.statementprocessor.api.dto.ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported file format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.bank.statementprocessor.api.dto.ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.bank.statementprocessor.api.dto.ProblemDetail.class)
                    )
            )
    })
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValidationReportDTO validateTransactionRecords(
            @Parameter(
                    description = "CSV or JSON file containing transaction records",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file) {
        
        String correlationId = MDC.get("correlationId");
        
        log.info("Received validation request, size: {} bytes", file.getSize());
        
        if (file.isEmpty()) {
            log.warn("Empty file received");
            throw new EmptyFileException("File is empty");
        }
        
        ValidationReport report = validationService.validateTransactionRecords(file);
        ValidationReportDTO dto = validationReportMapper.toDTO(report);

        dto.setCorrelationId(correlationId);
        
        log.info("Validation completed. Valid: {}, Failed: {}/{}", 
                dto.isValid(), dto.getFailedRecords(), dto.getTotalRecords());
        
        return dto;
    }
}

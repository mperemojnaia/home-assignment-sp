package com.bank.statementprocessor.service;

import com.bank.statementprocessor.domain.FileFormat;
import com.bank.statementprocessor.domain.TransactionRecord;
import com.bank.statementprocessor.domain.ValidationReport;
import com.bank.statementprocessor.exception.ParseException;
import com.bank.statementprocessor.exception.UnsupportedFileFormatException;
import com.bank.statementprocessor.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;



/**
 * Core service orchestrating the validation workflow.
 * Coordinates file parsing and validation rule application.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionRecordValidationService {
    
    private final FileParserService fileParserService;
    private final ValidationRuleEngine validationRuleEngine;
    
    /**
     * Validates a transaction record file by parsing it and applying validation rules.
     * 
     * All exceptions are runtime exceptions that will be handled by GlobalExceptionHandler:
     * - ParseException: File parsing failures (malformed CSV/JSON)
     * - UnsupportedFileFormatException: Unsupported file format
     * - ValidationException: Unexpected validation errors
     *
     * @param file the uploaded file to validate
     * @return ValidationReport containing validation results
     */
    public ValidationReport validateTransactionRecords(MultipartFile file) {
        log.info("Starting validation, size: {} bytes", file.getSize());
        
        try {
            FileFormat format = fileParserService.detectFormat(file);
            log.debug("Detected file format: {}", format);

            List<TransactionRecord> records = fileParserService.parseFile(file, format);
            log.info("Successfully parsed {} transaction records", records.size());
            
            ValidationReport report = validationRuleEngine.validate(records);
            log.info("Validation complete. Valid: {}, Failed records: {}/{}", 
                    report.isValid(), report.getFailedRecords(), report.getTotalRecords());
            
            return report;
            
        } catch (ParseException | UnsupportedFileFormatException e) {
            // Client error - malformed file or unsupported format, use warn
            log.warn("Client error during validation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Server error - unexpected failure
            log.error("Unexpected error during validation", e);
            throw new ValidationException("Unexpected error during validation", e);
        }
    }
}

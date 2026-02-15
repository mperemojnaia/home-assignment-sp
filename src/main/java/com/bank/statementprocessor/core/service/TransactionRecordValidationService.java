package com.bank.statementprocessor.core.service;

import com.bank.statementprocessor.core.model.FileFormat;
import com.bank.statementprocessor.core.model.TransactionRecord;
import com.bank.statementprocessor.core.model.ValidationReport;
import com.bank.statementprocessor.api.error.ParseException;
import com.bank.statementprocessor.api.error.UnsupportedFileFormatException;
import com.bank.statementprocessor.api.error.ValidationException;
import com.bank.statementprocessor.infrastructure.parser.FileParserService;
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
            log.warn("Client error during validation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during validation", e);
            throw new ValidationException("Unexpected error during validation", e);
        }
    }
}

package com.bank.statementprocessor.service;

import com.bank.statementprocessor.domain.FileFormat;
import com.bank.statementprocessor.domain.TransactionRecord;
import com.bank.statementprocessor.domain.ValidationFailure;
import com.bank.statementprocessor.domain.ValidationReport;
import com.bank.statementprocessor.exception.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StatementValidationService.
 * Tests the complete validation workflow with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class TransactionRecordValidationServiceTest {
    
    @Mock
    private FileParserService fileParserService;
    
    @Mock
    private ValidationRuleEngine validationRuleEngine;
    
    @Mock
    private MultipartFile mockFile;
    
    @InjectMocks
    private TransactionRecordValidationService validationService;
    
    @BeforeEach
    void setUp() {
        // No longer need to mock getOriginalFilename() since we removed PII logging
    }
    
    @Test
    void validateStatement_withValidFile_shouldReturnValidReport() throws IOException, ParseException {
        // Given
        List<TransactionRecord> records = Arrays.asList(
            TransactionRecord.builder()
                .reference(194261L)
                .accountNumber("NL91RABO0315273637")
                .description("Clothes from Jan Bakker")
                .startBalance(new BigDecimal("21.60"))
                .mutation(new BigDecimal("-41.83"))
                .endBalance(new BigDecimal("-20.23"))
                .build(),
            TransactionRecord.builder()
                .reference(112806L)
                .accountNumber("NL27SNSB0917829871")
                .description("Clothes from Richard de Vries")
                .startBalance(new BigDecimal("91.23"))
                .mutation(new BigDecimal("15.57"))
                .endBalance(new BigDecimal("106.80"))
                .build()
        );
        
        ValidationReport expectedReport = ValidationReport.builder()
            .valid(true)
            .totalRecords(2)
            .failedRecords(0)
            .failures(Collections.emptyList())
            .build();
        
        when(fileParserService.detectFormat(mockFile)).thenReturn(FileFormat.CSV);
        when(fileParserService.parseFile(mockFile, FileFormat.CSV)).thenReturn(records);
        when(validationRuleEngine.validate(records)).thenReturn(expectedReport);
        
        // When
        ValidationReport result = validationService.validateTransactionRecords(mockFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getTotalRecords()).isEqualTo(2);
        assertThat(result.getFailedRecords()).isEqualTo(0);
        assertThat(result.getFailures()).isEmpty();
        
        verify(fileParserService).detectFormat(mockFile);
        verify(fileParserService).parseFile(mockFile, FileFormat.CSV);
        verify(validationRuleEngine).validate(records);
    }
    
    @Test
    void validateStatement_withValidationFailures_shouldReturnReportWithFailures() throws IOException, ParseException {
        // Given
        List<TransactionRecord> records = Arrays.asList(
            TransactionRecord.builder()
                .reference(112806L)
                .accountNumber("NL27SNSB0917829871")
                .description("Clothes from Richard de Vries")
                .startBalance(new BigDecimal("91.23"))
                .mutation(new BigDecimal("15.57"))
                .endBalance(new BigDecimal("106.80"))
                .build(),
            TransactionRecord.builder()
                .reference(112806L)
                .accountNumber("NL69ABNA0433647324")
                .description("Clothes for Willem Dekker")
                .startBalance(new BigDecimal("86.66"))
                .mutation(new BigDecimal("44.50"))
                .endBalance(new BigDecimal("131.16"))
                .build(),
            TransactionRecord.builder()
                .reference(194261L)
                .accountNumber("NL91RABO0315273637")
                .description("Clothes from Jan Bakker")
                .startBalance(new BigDecimal("21.60"))
                .mutation(new BigDecimal("-41.83"))
                .endBalance(new BigDecimal("-20.00")) // Incorrect balance
                .build()
        );
        
        List<ValidationFailure> failures = Arrays.asList(
            ValidationFailure.builder()
                .reference(112806L)
                .description("Clothes from Richard de Vries")
                .reasons(new HashSet<>(Collections.singletonList(ValidationFailure.FailureReason.DUPLICATE_REFERENCE)))
                .build(),
            ValidationFailure.builder()
                .reference(112806L)
                .description("Clothes for Willem Dekker")
                .reasons(new HashSet<>(Collections.singletonList(ValidationFailure.FailureReason.DUPLICATE_REFERENCE)))
                .build(),
            ValidationFailure.builder()
                .reference(194261L)
                .description("Clothes from Jan Bakker")
                .reasons(new HashSet<>(Collections.singletonList(ValidationFailure.FailureReason.INCORRECT_END_BALANCE)))
                .build()
        );
        
        ValidationReport expectedReport = ValidationReport.builder()
            .valid(false)
            .totalRecords(3)
            .failedRecords(3)
            .failures(failures)
            .build();
        
        when(fileParserService.detectFormat(mockFile)).thenReturn(FileFormat.JSON);
        when(fileParserService.parseFile(mockFile, FileFormat.JSON)).thenReturn(records);
        when(validationRuleEngine.validate(records)).thenReturn(expectedReport);
        
        // When
        ValidationReport result = validationService.validateTransactionRecords(mockFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getTotalRecords()).isEqualTo(3);
        assertThat(result.getFailedRecords()).isEqualTo(3);
        assertThat(result.getFailures()).hasSize(3);
        
        verify(fileParserService).detectFormat(mockFile);
        verify(fileParserService).parseFile(mockFile, FileFormat.JSON);
        verify(validationRuleEngine).validate(records);
    }
    
    @Test
    void validateStatement_withParseException_shouldPropagateException() throws IOException, ParseException {
        // Given
        ParseException parseException = ParseException.builder()
                .message("Invalid CSV format")
                .lineNumber(5)
                .build();
        
        when(fileParserService.detectFormat(mockFile)).thenReturn(FileFormat.CSV);
        when(fileParserService.parseFile(mockFile, FileFormat.CSV)).thenThrow(parseException);
        
        // When/Then
        assertThatThrownBy(() -> validationService.validateTransactionRecords(mockFile))
            .isInstanceOf(ParseException.class)
            .hasMessageContaining("Invalid CSV format");
        
        verify(fileParserService).detectFormat(mockFile);
        verify(fileParserService).parseFile(mockFile, FileFormat.CSV);
    }
    
    @Test
    void validateStatement_withIOException_shouldWrapInParseException() throws ParseException {
        // Given - IO errors are now wrapped in ParseException
        ParseException parseException = new ParseException("Failed to read file: IO error", new IOException("IO error"));
        
        when(fileParserService.detectFormat(mockFile)).thenReturn(FileFormat.CSV);
        when(fileParserService.parseFile(mockFile, FileFormat.CSV)).thenThrow(parseException);
        
        // When/Then
        assertThatThrownBy(() -> validationService.validateTransactionRecords(mockFile))
            .isInstanceOf(ParseException.class)
            .hasMessageContaining("Failed to read file");
        
        verify(fileParserService).detectFormat(mockFile);
        verify(fileParserService).parseFile(mockFile, FileFormat.CSV);

    }
    
    @Test
    void validateStatement_withUnexpectedException_shouldWrapInValidationException() throws IOException, ParseException {
        // Given
        RuntimeException unexpectedException = new RuntimeException("Unexpected error");
        
        when(fileParserService.detectFormat(mockFile)).thenReturn(FileFormat.CSV);
        when(fileParserService.parseFile(mockFile, FileFormat.CSV)).thenThrow(unexpectedException);
        
        // When/Then
        assertThatThrownBy(() -> validationService.validateTransactionRecords(mockFile))
            .isInstanceOf(com.bank.statementprocessor.exception.ValidationException.class)
            .hasMessage("Unexpected error during validation")
            .hasCause(unexpectedException);
        
        verify(fileParserService).detectFormat(mockFile);
        verify(fileParserService).parseFile(mockFile, FileFormat.CSV);
    }
}

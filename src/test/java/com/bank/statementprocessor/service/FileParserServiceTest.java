package com.bank.statementprocessor.service;

import com.bank.statementprocessor.domain.FileFormat;
import com.bank.statementprocessor.domain.TransactionRecord;
import com.bank.statementprocessor.exception.ParseException;
import com.bank.statementprocessor.testutil.CsvContentBuilder;
import com.bank.statementprocessor.testutil.JsonContentBuilder;
import com.bank.statementprocessor.testutil.MockMultipartFileBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.bank.statementprocessor.testutil.CsvContentBuilder.aCsvContent;
import static com.bank.statementprocessor.testutil.JsonContentBuilder.aJsonContent;
import static com.bank.statementprocessor.testutil.MockMultipartFileBuilder.aCsvFile;
import static com.bank.statementprocessor.testutil.MockMultipartFileBuilder.aJsonFile;
import static com.bank.statementprocessor.testutil.MockMultipartFileBuilder.aMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for FileParserService.
 */
class FileParserServiceTest {
    
    private FileParserService fileParserService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        fileParserService = new FileParserService(objectMapper);
    }
    
    // ========== Format Detection Tests ==========
    
    @Test
    void testDetectFormat_CsvContentType() {
        MultipartFile file = aCsvFile()
                .withContentType("text/csv")
                .build();
        
        FileFormat format = fileParserService.detectFormat(file);
        
        assertThat(format).isEqualTo(FileFormat.CSV);
    }
    
    @Test
    void testDetectFormat_JsonContentType() {
        MultipartFile file = aJsonFile()
                .withContentType("application/json")
                .build();
        
        FileFormat format = fileParserService.detectFormat(file);
        
        assertThat(format).isEqualTo(FileFormat.JSON);
    }
    
    @Test
    void testDetectFormat_CsvExtension() {
        MultipartFile file = aCsvFile()
                .withContentType("application/octet-stream")
                .build();
        
        FileFormat format = fileParserService.detectFormat(file);
        
        assertThat(format).isEqualTo(FileFormat.CSV);
    }
    
    @Test
    void testDetectFormat_JsonExtension() {
        MultipartFile file = aJsonFile()
                .withContentType("application/octet-stream")
                .build();
        
        FileFormat format = fileParserService.detectFormat(file);
        
        assertThat(format).isEqualTo(FileFormat.JSON);
    }
    
    @Test
    void testDetectFormat_ApplicationCsvContentType() {
        MultipartFile file = aCsvFile()
                .withContentType("application/csv")
                .build();
        
        FileFormat format = fileParserService.detectFormat(file);
        
        assertThat(format).isEqualTo(FileFormat.CSV);
    }
    
    @Test
    void testDetectFormat_CsvExtensionUpperCase() {
        MultipartFile file = aMultipartFile()
                .withOriginalFilename("TEST.CSV")
                .withContentType("application/octet-stream")
                .build();
        
        FileFormat format = fileParserService.detectFormat(file);
        
        assertThat(format).isEqualTo(FileFormat.CSV);
    }
    
    @Test
    void testDetectFormat_JsonExtensionUpperCase() {
        MultipartFile file = aMultipartFile()
                .withOriginalFilename("TEST.JSON")
                .withContentType("application/octet-stream")
                .build();
        
        FileFormat format = fileParserService.detectFormat(file);
        
        assertThat(format).isEqualTo(FileFormat.JSON);
    }
    
    @Test
    void testDetectFormat_UnsupportedExtension() {
        MultipartFile file = aMultipartFile()
                .withOriginalFilename("test.txt")
                .withContentType("text/plain")
                .build();
        
        assertThatThrownBy(() -> fileParserService.detectFormat(file))
                .isInstanceOf(com.bank.statementprocessor.exception.UnsupportedFileFormatException.class)
                .hasMessageContaining("Unsupported file format");
    }
    
    @Test
    void testDetectFormat_NoFilename() {
        MultipartFile file = aMultipartFile()
                .withOriginalFilename(null)
                .withContentType("text/plain")
                .build();
        
        assertThatThrownBy(() -> fileParserService.detectFormat(file))
                .isInstanceOf(com.bank.statementprocessor.exception.UnsupportedFileFormatException.class)
                .hasMessageContaining("Unsupported file format");
    }
    
    @Test
    void testDetectFormat_NoExtension() {
        MultipartFile file = aMultipartFile()
                .withOriginalFilename("testfile")
                .withContentType("application/octet-stream")
                .build();
        
        assertThatThrownBy(() -> fileParserService.detectFormat(file))
                .isInstanceOf(com.bank.statementprocessor.exception.UnsupportedFileFormatException.class)
                .hasMessageContaining("Unsupported file format");
    }
    
    // ========== CSV Parsing - Valid Files ==========
    
    @Test
    void testParseCsv_ValidFile() throws ParseException, IOException {
        byte[] csvContent = Files.readAllBytes(Paths.get("test-data/records.csv"));
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        List<TransactionRecord> records = fileParserService.parseFile(file, FileFormat.CSV);
        
        assertThat(records).isNotEmpty();
        assertThat(records).hasSize(10);
        
        // Verify first record
        TransactionRecord firstRecord = records.get(0);
        assertThat(firstRecord.getReference()).isEqualTo(194261L);
        assertThat(firstRecord.getAccountNumber()).isEqualTo("NL91RABO0315273637");
        assertThat(firstRecord.getDescription()).isEqualTo("Book John Smith");
        assertThat(firstRecord.getStartBalance()).isEqualByComparingTo(new BigDecimal("21.6"));
        assertThat(firstRecord.getMutation()).isEqualByComparingTo(new BigDecimal("-41.83"));
        assertThat(firstRecord.getEndBalance()).isEqualByComparingTo(new BigDecimal("-20.23"));
        
        // Verify duplicate reference exists (112806 appears 3 times)
        long count112806 = records.stream()
                .filter(r -> r.getReference().equals(112806L))
                .count();
        assertThat(count112806).isEqualTo(3);
    }
    
    @Test
    void testParseCsv_OnlyHeaders() throws ParseException {
        String csvContent = aCsvContent().build();  // Just headers, no rows
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        List<TransactionRecord> records = fileParserService.parseFile(file, FileFormat.CSV);
        
        assertThat(records).isEmpty();
    }
    
    @Test
    void testParseCsv_ValidRecordWithWhitespace() throws ParseException {
        String csvContent = aCsvContent()
                .withRow("194261", "  NL91RABO0315273637  ", "  Test Description  ", "21.6", "-41.83", "-20.23")
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        List<TransactionRecord> records = fileParserService.parseFile(file, FileFormat.CSV);
        
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getAccountNumber()).isEqualTo("NL91RABO0315273637");
        assertThat(records.get(0).getDescription()).isEqualTo("Test Description");
    }
    
    @Test
    void testParseCsv_MutationWithPlusSign() throws ParseException {
        String csvContent = aCsvContent()
                .withRow("194261", "NL91RABO0315273637", "Test", "100.00", "+50.00", "150.00")
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        List<TransactionRecord> records = fileParserService.parseFile(file, FileFormat.CSV);
        
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getMutation()).isEqualByComparingTo(new BigDecimal("50.00"));
    }
    
    @Test
    void testParseCsv_HeadersWithDifferentSpacing() throws ParseException {
        String csvContent = aCsvContent()
                .withCustomHeaders("Reference", "Account Number", "Description", "StartBalance", "Mutation", "EndBalance")
                .withRow("194261", "NL91RABO0315273637", "Test", "21.6", "-41.83", "-20.23")
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        List<TransactionRecord> records = fileParserService.parseFile(file, FileFormat.CSV);
        
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getReference()).isEqualTo(194261L);
    }
    
    // ========== CSV Parsing - Error Cases ==========
    
    @Test
    void testParseCsv_EmptyFile() {
        MultipartFile file = aCsvFile()
                .withContent("")
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class);
    }
    
    @Test
    void testParseCsv_MissingHeader() {
        // Manually create CSV with missing Description header
        String csvContent = "Reference,AccountNumber,Start Balance,Mutation,End Balance\n" +
                "194261,NL91RABO0315273637,21.6,-41.83,-20.23";
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Missing required header: Description");
    }
    
    @Test
    void testParseCsv_MissingRequiredField() {
        String csvContent = aCsvContent()
                .withEmptyFieldRow("accountNumber")
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Empty required field");
    }
    
    @Test
    void testParseCsv_EmptyDescription() {
        String csvContent = aCsvContent()
                .withEmptyFieldRow("description")
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Empty required field");
    }
    
    @Test
    void testParseCsv_WhitespaceOnlyDescription() {
        String csvContent = aCsvContent()
                .withRow("194261", "NL91RABO0315273637", "   ", "21.6", "-41.83", "-20.23")
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Empty required field");
    }
    
    @Test
    void testParseCsv_InvalidNumberFormat() {
        String csvContent = aCsvContent()
                .withInvalidReferenceRow()
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid reference number");
    }
    
    @Test
    void testParseCsv_InvalidStartBalance() {
        String csvContent = aCsvContent()
                .withInvalidNumberRow("startBalance")
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid start balance");
    }
    
    @Test
    void testParseCsv_InvalidMutation() {
        String csvContent = aCsvContent()
                .withInvalidNumberRow("mutation")
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid mutation amount");
    }
    
    @Test
    void testParseCsv_InvalidEndBalance() {
        String csvContent = aCsvContent()
                .withInvalidNumberRow("endBalance")
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid end balance");
    }
    
    @Test
    void testParseCsv_MultipleRecordsWithOneInvalid() {
        String csvContent = aCsvContent()
                .withValidRow()
                .withInvalidReferenceRow()
                .withValidRow()
                .build();
        
        MultipartFile file = aCsvFile()
                .withContent(csvContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.CSV))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid reference number at line 3");
    }
    
    // ========== JSON Parsing - Valid Files ==========
    
    @Test
    void testParseJson_ValidFile() throws ParseException, IOException {
        byte[] jsonContent = Files.readAllBytes(Paths.get("test-data/records.json"));
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        List<TransactionRecord> records = fileParserService.parseFile(file, FileFormat.JSON);
        
        assertThat(records).isNotEmpty();
        assertThat(records).hasSize(10);
        
        // Verify first record
        TransactionRecord firstRecord = records.get(0);
        assertThat(firstRecord.getReference()).isEqualTo(130498L);
        assertThat(firstRecord.getAccountNumber()).isEqualTo("NL69ABNA0433647324");
        assertThat(firstRecord.getDescription()).isEqualTo("Book Jan Theuß");
        assertThat(firstRecord.getStartBalance()).isEqualByComparingTo(new BigDecimal("26.9"));
        assertThat(firstRecord.getMutation()).isEqualByComparingTo(new BigDecimal("-18.78"));
        assertThat(firstRecord.getEndBalance()).isEqualByComparingTo(new BigDecimal("8.12"));
    }
    
    @Test
    void testParseJson_EmptyArray() throws ParseException {
        MultipartFile file = aJsonFile()
                .withContent("[]")
                .build();
        
        List<TransactionRecord> records = fileParserService.parseFile(file, FileFormat.JSON);
        
        assertThat(records).isEmpty();
    }
    
    @Test
    void testParseJson_ValidRecordWithWhitespace() throws ParseException {
        String jsonContent = "[{" +
                "\"reference\": 123," +
                "\"accountNumber\": \"  NL123  \"," +
                "\"description\": \"  Test Description  \"," +
                "\"startBalance\": 100.0," +
                "\"mutation\": 50.0," +
                "\"endBalance\": 150.0" +
                "}]";
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        List<TransactionRecord> records = fileParserService.parseFile(file, FileFormat.JSON);
        
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getAccountNumber()).isEqualTo("NL123");
        assertThat(records.get(0).getDescription()).isEqualTo("Test Description");
    }
    
    // ========== JSON Parsing - Error Cases ==========
    
    @Test
    void testParseJson_MalformedJson() {
        MultipartFile file = aJsonFile()
                .withContent("{ invalid json }")
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Malformed JSON");
    }
    
    @Test
    void testParseJson_NotAnArray() {
        String jsonContent = aJsonContent()
                .withValidRecord()
                .buildAsObject();  // Returns object instead of array
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("JSON root element must be an array");
    }
    
    @Test
    void testParseJson_MissingRequiredField() {
        String jsonContent = aJsonContent()
                .withMissingField("description")
                .build();
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Missing required field");
    }
    
    @Test
    void testParseJson_NullReference() {
        String jsonContent = aJsonContent()
                .withNullField("reference")
                .build();
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Missing required field 'reference'");
    }
    
    @Test
    void testParseJson_NullAccountNumber() {
        String jsonContent = aJsonContent()
                .withNullField("accountNumber")
                .build();
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Missing required field 'accountNumber'");
    }
    
    @Test
    void testParseJson_EmptyAccountNumber() {
        String jsonContent = aJsonContent()
                .withEmptyField("accountNumber")
                .build();
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Missing required field 'accountNumber'");
    }
    
    @Test
    void testParseJson_WhitespaceOnlyDescription() {
        String jsonContent = aJsonContent()
                .withEmptyField("description")
                .build();
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Missing required field 'description'");
    }
    
    @Test
    void testParseJson_InvalidStartBalance() {
        String jsonContent = aJsonContent()
                .withInvalidNumberField("startBalance")
                .build();
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid startBalance");
    }
    
    @Test
    void testParseJson_InvalidMutation() {
        String jsonContent = aJsonContent()
                .withInvalidNumberField("mutation")
                .build();
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid mutation");
    }
    
    @Test
    void testParseJson_InvalidEndBalance() {
        String jsonContent = aJsonContent()
                .withInvalidNumberField("endBalance")
                .build();
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid endBalance");
    }
    
    @Test
    void testParseJson_MultipleRecordsWithOneInvalid() {
        String jsonContent = aJsonContent()
                .withValidRecord()
                .withInvalidNumberField("startBalance")
                .withValidRecord()
                .build();
        
        MultipartFile file = aJsonFile()
                .withContent(jsonContent)
                .build();
        
        assertThatThrownBy(() -> fileParserService.parseFile(file, FileFormat.JSON))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid startBalance");
    }
}

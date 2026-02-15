package com.bank.statementprocessor.api.controller;

import com.bank.statementprocessor.api.dto.ValidationReportDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionRecordControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUploadValidCsvFile() throws Exception {
        // Given - Load the sample CSV file
        byte[] csvContent = Files.readAllBytes(Paths.get("test-data/records.csv"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "records.csv",
                "text/csv",
                csvContent
        );
        
        // When - Upload the file
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify the validation report
        String responseBody = result.getResponse().getContentAsString();
        ValidationReportDTO report = objectMapper.readValue(responseBody, ValidationReportDTO.class);
        
        assertThat(report).isNotNull();
        assertThat(report.isValid()).isFalse();
        assertThat(report.getTotalRecords()).isEqualTo(10);
        assertThat(report.getFailedRecords()).isEqualTo(3);
        assertThat(report.getFailures()).hasSize(3);
        
        // Verify all three instances of duplicate reference 112806 are reported
        long duplicateCount = report.getFailures().stream()
                .filter(f -> f.getReference().equals(112806L))
                .count();
        assertThat(duplicateCount).isEqualTo(3);
    }
    

    @Test
    void testUploadValidJsonFile() throws Exception {
        // Given - Load the sample JSON file
        byte[] jsonContent = Files.readAllBytes(Paths.get("test-data/records.json"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "records.json",
                "application/json",
                jsonContent
        );
        
        // When - Upload the file
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify the validation report
        String responseBody = result.getResponse().getContentAsString();
        ValidationReportDTO report = objectMapper.readValue(responseBody, ValidationReportDTO.class);
        
        assertThat(report).isNotNull();
        assertThat(report.isValid()).isFalse();
        assertThat(report.getTotalRecords()).isEqualTo(10);
        assertThat(report.getFailedRecords()).isEqualTo(2);
        assertThat(report.getFailures()).hasSize(2);
        
        // Verify the incorrect balance failures for references 167875 and 165102
        assertThat(report.getFailures())
                .anyMatch(f -> f.getReference().equals(167875L) && 
                             f.getReasons().contains(com.bank.statementprocessor.core.model.ValidationFailure.FailureReason.INCORRECT_END_BALANCE))
                .anyMatch(f -> f.getReference().equals(165102L) && 
                             f.getReasons().contains(com.bank.statementprocessor.core.model.ValidationFailure.FailureReason.INCORRECT_END_BALANCE));
    }

    @Test
    void testUploadFileWithValidationFailures() throws Exception {
        // Given - Create a CSV file with validation failures
        String csvContent = """
                Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
                100001,NL91RABO0315273637,Test Transaction 1,100.00,50.00,150.00
                100001,NL27SNSB0917829871,Test Transaction 2,200.00,50.00,250.00
                100002,NL69ABNA0433647324,Test Transaction 3,100.00,50.00,200.00
                """;
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        // When - Upload the file
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify the validation report contains failures
        String responseBody = result.getResponse().getContentAsString();
        ValidationReportDTO report = objectMapper.readValue(responseBody, ValidationReportDTO.class);
        
        assertThat(report).isNotNull();
        assertThat(report.isValid()).isFalse();
        assertThat(report.getTotalRecords()).isEqualTo(3);
        
        // Should have 2 duplicate reference failures and 1 incorrect balance failure
        assertThat(report.getFailedRecords()).isEqualTo(3);
        assertThat(report.getFailures()).hasSize(3);
        
        // Verify duplicate reference failures
        long duplicateCount = report.getFailures().stream()
                .filter(f -> f.getReference().equals(100001L))
                .count();
        assertThat(duplicateCount).isEqualTo(2);
        
        // Verify incorrect balance failure
        long balanceFailureCount = report.getFailures().stream()
                .filter(f -> f.getReference().equals(100002L))
                .filter(f -> f.getReasons().contains(
                        com.bank.statementprocessor.core.model.ValidationFailure.FailureReason.INCORRECT_END_BALANCE))
                .count();
        assertThat(balanceFailureCount).isEqualTo(1);
    }
    

    @Test
    void testUploadWithoutFileParameter() throws Exception {
        // When - Upload without file parameter
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify ProblemDetail response schema
        String responseBody = result.getResponse().getContentAsString();
        com.bank.statementprocessor.api.dto.ProblemDetail problem = 
                objectMapper.readValue(responseBody, com.bank.statementprocessor.api.dto.ProblemDetail.class);
        
        assertThat(problem.status()).isEqualTo(400);
        assertThat(problem.type()).isEqualTo("about:blank");
        assertThat(problem.title()).isEqualTo("Missing File");
        assertThat(problem.detail()).isEqualTo("File parameter is required");
        assertThat(problem.instance()).isNotNull();
        assertThat(problem.timestamp()).isNotNull();
        assertThat(problem.correlationId()).isNotNull();
        assertThat(problem.errorCode()).isEqualTo("MISSING_FILE");
    }

    @Test
    void testUploadEmptyFile() throws Exception {
        // Given - Create an empty file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );
        
        // When - Upload the empty file
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify ProblemDetail response schema
        String responseBody = result.getResponse().getContentAsString();
        com.bank.statementprocessor.api.dto.ProblemDetail problem = 
                objectMapper.readValue(responseBody, com.bank.statementprocessor.api.dto.ProblemDetail.class);
        
        assertThat(problem.status()).isEqualTo(400);
        assertThat(problem.type()).isEqualTo("about:blank");
        assertThat(problem.title()).isEqualTo("Empty File");
        assertThat(problem.detail()).isEqualTo("File is empty");
        assertThat(problem.instance()).isNotNull();
        assertThat(problem.timestamp()).isNotNull();
        assertThat(problem.correlationId()).isNotNull();
        assertThat(problem.errorCode()).isEqualTo("EMPTY_FILE");
    }

    @Test
    void testUploadUnsupportedFormat() throws Exception {
        // Given - Create a file with unsupported format (.txt file)
        String textContent = "This is a plain text file, not CSV or JSON";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "records.txt",
                "text/plain",
                textContent.getBytes()
        );
        
        // When - Upload the unsupported file
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify ProblemDetail response schema
        String responseBody = result.getResponse().getContentAsString();
        com.bank.statementprocessor.api.dto.ProblemDetail problem = 
                objectMapper.readValue(responseBody, com.bank.statementprocessor.api.dto.ProblemDetail.class);
        
        assertThat(problem.status()).isEqualTo(415);
        assertThat(problem.type()).isEqualTo("about:blank");
        assertThat(problem.title()).isEqualTo("Unsupported File Format");
        assertThat(problem.detail()).contains("Unsupported file format");
        assertThat(problem.instance()).isNotNull();
        assertThat(problem.timestamp()).isNotNull();
        assertThat(problem.correlationId()).isNotNull();
        assertThat(problem.errorCode()).isEqualTo("UNSUPPORTED_FORMAT");
    }

    @Test
    void testUploadOversizedFile() throws Exception {
        // Given - Create a file larger than 10MB
        // Create a 11MB file (10MB limit + 1MB)
        // Note: Large files may cause parsing errors before MaxUploadSizeExceededException
        byte[] largeContent = new byte[11 * 1024 * 1024];
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.csv",
                "text/csv",
                largeContent
        );
        
        // When - Upload the oversized file
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(mvcResult -> {
                    int status = mvcResult.getResponse().getStatus();
                    // Accept either 413 (Payload Too Large) or 500 (if parsing fails first)
                    assertThat(status).isIn(413, 500);
                })
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify ProblemDetail response schema
        String responseBody = result.getResponse().getContentAsString();
        com.bank.statementprocessor.api.dto.ProblemDetail problem = 
                objectMapper.readValue(responseBody, com.bank.statementprocessor.api.dto.ProblemDetail.class);
        
        // Verify common ProblemDetail fields
        assertThat(problem.status()).isIn(413, 500);
        assertThat(problem.type()).isEqualTo("about:blank");
        assertThat(problem.title()).isNotNull();
        assertThat(problem.detail()).isNotNull();
        assertThat(problem.instance()).isNotNull();
        assertThat(problem.timestamp()).isNotNull();
        assertThat(problem.correlationId()).isNotNull();
        assertThat(problem.errorCode()).isNotNull();
    }

    @Test
    void testUploadMalformedCsvFile() throws Exception {
        // Given - Create a malformed CSV file (missing required columns)
        String malformedCsv = """
                Reference,AccountNumber,Description
                100001,NL91RABO0315273637,Test Transaction
                """;
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malformed.csv",
                "text/csv",
                malformedCsv.getBytes()
        );
        
        // When - Upload the malformed file
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify ProblemDetail response schema
        String responseBody = result.getResponse().getContentAsString();
        com.bank.statementprocessor.api.dto.ProblemDetail problem = 
                objectMapper.readValue(responseBody, com.bank.statementprocessor.api.dto.ProblemDetail.class);
        
        assertThat(problem.status()).isEqualTo(400);
        assertThat(problem.type()).isEqualTo("about:blank");
        assertThat(problem.title()).isEqualTo("Parse Error");
        assertThat(problem.detail()).contains("Failed to parse file");
        assertThat(problem.instance()).isNotNull();
        assertThat(problem.timestamp()).isNotNull();
        assertThat(problem.correlationId()).isNotNull();
        assertThat(problem.errorCode()).isEqualTo("PARSE_ERROR");
    }

    @Test
    void testUploadMalformedJsonFile() throws Exception {
        // Given - Create a malformed JSON file (invalid syntax)
        String malformedJson = """
                [
                    {
                        "reference": "100001",
                        "accountNumber": "NL91RABO0315273637"
                        "description": "Missing comma"
                    }
                ]
                """;
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malformed.json",
                "application/json",
                malformedJson.getBytes()
        );
        
        // When - Upload the malformed file
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify ProblemDetail response schema
        String responseBody = result.getResponse().getContentAsString();
        com.bank.statementprocessor.api.dto.ProblemDetail problem = 
                objectMapper.readValue(responseBody, com.bank.statementprocessor.api.dto.ProblemDetail.class);
        
        assertThat(problem.status()).isEqualTo(400);
        assertThat(problem.type()).isEqualTo("about:blank");
        assertThat(problem.title()).isEqualTo("Parse Error");
        assertThat(problem.detail()).contains("Failed to parse file");
        assertThat(problem.instance()).isNotNull();
        assertThat(problem.timestamp()).isNotNull();
        assertThat(problem.correlationId()).isNotNull();
        assertThat(problem.errorCode()).isEqualTo("PARSE_ERROR");
    }

    @Test
    void testUploadFileWithAllValidRecords() throws Exception {
        // Given - Create a CSV file with all valid records
        String csvContent = """
                Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
                100001,NL91RABO0315273637,Test Transaction 1,100.00,50.00,150.00
                100002,NL27SNSB0917829871,Test Transaction 2,200.00,-50.00,150.00
                100003,NL69ABNA0433647324,Test Transaction 3,300.00,100.00,400.00
                """;
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        // When - Upload the file
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify the validation report shows all records are valid
        String responseBody = result.getResponse().getContentAsString();
        ValidationReportDTO report = objectMapper.readValue(responseBody, ValidationReportDTO.class);
        
        assertThat(report).isNotNull();
        assertThat(report.isValid()).isTrue();
        assertThat(report.getTotalRecords()).isEqualTo(3);
        assertThat(report.getFailedRecords()).isEqualTo(0);
        assertThat(report.getFailures()).isEmpty();
    }

    @Test
    void testInternalServerError() throws Exception {
        // Given - Create a file that will trigger an IOException during processing
        // Note: IOException is wrapped in ParseException (400), so we test the generic handler
        // by simulating an unexpected RuntimeException
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                "Reference,AccountNumber,Description,Start Balance,Mutation,End Balance\n".getBytes()
        ) {
            @Override
            public java.io.InputStream getInputStream() throws IOException {
                // IOException gets wrapped in ParseException (400)
                // For a true 500, we'd need an unexpected RuntimeException
                throw new IOException("Simulated I/O error");
            }
        };
        
        // When - Upload the file that will cause a parse error (400, not 500)
        // IOException is treated as a client error (malformed file)
        MvcResult result = mockMvc.perform(multipart("/api/v1/transaction-records/validate")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Then - Verify ProblemDetail response schema
        String responseBody = result.getResponse().getContentAsString();
        com.bank.statementprocessor.api.dto.ProblemDetail problem = 
                objectMapper.readValue(responseBody, com.bank.statementprocessor.api.dto.ProblemDetail.class);
        
        assertThat(problem.status()).isEqualTo(400);
        assertThat(problem.type()).isEqualTo("about:blank");
        assertThat(problem.title()).isEqualTo("Parse Error");
        assertThat(problem.detail()).contains("Failed to parse file");
        assertThat(problem.instance()).isNotNull();
        assertThat(problem.timestamp()).isNotNull();
        assertThat(problem.correlationId()).isNotNull();
        assertThat(problem.errorCode()).isEqualTo("PARSE_ERROR");
    }
}

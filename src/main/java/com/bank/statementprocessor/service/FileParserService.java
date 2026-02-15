package com.bank.statementprocessor.service;

import com.bank.statementprocessor.domain.FileFormat;
import com.bank.statementprocessor.domain.TransactionRecord;
import com.bank.statementprocessor.exception.ParseException;
import com.bank.statementprocessor.exception.UnsupportedFileFormatException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for parsing transaction files in CSV and JSON formats.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileParserService {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Parses a file into a list of transaction records.
     *
     * @param file the uploaded file
     * @param format the detected file format
     * @return list of parsed transaction records
     * @throws ParseException if the file cannot be parsed (includes IO errors)
     */
    public List<TransactionRecord> parseFile(MultipartFile file, FileFormat format) 
            throws ParseException {
        log.info("Parsing file with format: {}, size: {} bytes", format, file.getSize());
        
        try (InputStream inputStream = file.getInputStream()) {
            List<TransactionRecord> records = switch (format) {
                case CSV -> parseCsv(inputStream);
                case JSON -> parseJson(inputStream);
            };
            
            log.info("Successfully parsed {} records", records.size());
            return records;
        } catch (ParseException e) {
            log.warn("Failed to parse file: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("I/O error while parsing file", e);
            throw new ParseException("Failed to read file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Detects the file format based on content type and file extension.
     *
     * @param file the uploaded file
     * @return the detected file format
     */
    public FileFormat detectFormat(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        log.debug("Detecting format with content type: {}", contentType);
        
        if (contentType != null) {
            if (contentType.equals("text/csv") || contentType.equals("application/csv")) {
                log.debug("Detected CSV format from content type");
                return FileFormat.CSV;
            }
            if (contentType.equals("application/json")) {
                log.debug("Detected JSON format from content type");
                return FileFormat.JSON;
            }
        }
        
        if (filename != null) {
            String extension = filename.substring(Math.max(0, filename.lastIndexOf('.')));
            if (filename.toLowerCase().endsWith(".csv")) {
                log.debug("Detected CSV format from file extension");
                return FileFormat.CSV;
            }
            if (filename.toLowerCase().endsWith(".json")) {
                log.debug("Detected JSON format from file extension");
                return FileFormat.JSON;
            }
            log.warn("Unsupported file extension: {}, content type: {}", extension, contentType);
        } else {
            log.warn("Unsupported file format with content type: {}", contentType);
        }
        
        throw new UnsupportedFileFormatException("Unsupported file format. Only CSV and JSON are supported");
    }
    
    /**
     * Parses a CSV file into transaction records.
     *
     * @param inputStream the input stream to read from
     * @return list of parsed transaction records
     * @throws ParseException if the CSV cannot be parsed (includes IO errors)
     */
    private List<TransactionRecord> parseCsv(InputStream inputStream) 
            throws ParseException {
        log.debug("Starting CSV parsing");
        
        List<TransactionRecord> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();
            
            try (CSVParser csvParser = csvFormat.parse(reader)) {
                validateCsvHeaders(csvParser);
                
                int lineNumber = 2;
                for (CSVRecord csvRecord : csvParser) {
                    try {
                        TransactionRecord record = parseCsvRecord(csvRecord, lineNumber);
                        records.add(record);
                        lineNumber++;
                    } catch (NumberFormatException e) {
                        throw ParseException.builder()
                                .message("Invalid number format at line " + lineNumber)
                                .lineNumber(lineNumber)
                                .build();
                    } catch (IllegalArgumentException e) {
                        throw ParseException.builder()
                                .message("Invalid data at line " + lineNumber + ": " + e.getMessage())
                                .lineNumber(lineNumber)
                                .build();
                    }
                }
                
                log.debug("Parsed {} records from CSV", records.size());
                return records;
            }
            
        } catch (IOException e) {
            // Server error - I/O failure
            log.error("I/O error while parsing CSV", e);
            throw new ParseException("Failed to read CSV file", e);
        }
    }
    
    /**
     * Validates that the CSV has the expected header columns.
     *
     * @param csvParser the CSV parser
     * @throws ParseException if required headers are missing
     */
    private void validateCsvHeaders(CSVParser csvParser) throws ParseException {
        List<String> headers = csvParser.getHeaderNames();
        
        String[] expectedHeaders = {
            "Reference", "AccountNumber", "Description", 
            "Start Balance", "Mutation", "End Balance"
        };
        
        for (String expectedHeader : expectedHeaders) {
            boolean found = headers.stream()
                    .anyMatch(h -> normalizeHeaderName(h).equals(normalizeHeaderName(expectedHeader)));
            
            if (!found) {
                throw ParseException.builder()
                        .message("Missing required header: " + expectedHeader)
                        .lineNumber(1)
                        .fieldName(expectedHeader)
                        .build();
            }
        }
    }
    
    /**
     * Normalizes a header name by removing spaces and converting to lowercase.
     *
     * @param header the header name
     * @return the normalized header name
     */
    private String normalizeHeaderName(String header) {
        return header.trim().replaceAll("\\s+", "").toLowerCase();
    }
    
    /**
     * Parses a single CSV record into a TransactionRecord.
     *
     * @param csvRecord the CSV record
     * @param lineNumber the line number for error reporting
     * @return the parsed transaction record
     * @throws ParseException if required fields are missing or invalid
     */
    private TransactionRecord parseCsvRecord(CSVRecord csvRecord, int lineNumber) 
            throws ParseException {
        
        Long reference = parseLongField(csvRecord, "Reference", lineNumber);
        String accountNumber = getRequiredField(csvRecord, "Account Number", lineNumber).trim();
        String description = getRequiredField(csvRecord, "Description", lineNumber).trim();
        BigDecimal startBalance = parseBigDecimalField(csvRecord, "Start Balance", lineNumber);
        BigDecimal mutation = parseMutationField(csvRecord, lineNumber);
        BigDecimal endBalance = parseBigDecimalField(csvRecord, "End Balance", lineNumber);
        
        return TransactionRecord.builder()
                .reference(reference)
                .accountNumber(accountNumber)
                .description(description)
                .startBalance(startBalance)
                .mutation(mutation)
                .endBalance(endBalance)
                .build();
    }
    
    /**
     * Parses a Long field from a CSV record.
     *
     * @param csvRecord the CSV record
     * @param fieldName the field name
     * @param lineNumber the line number for error reporting
     * @return the parsed Long value
     * @throws ParseException if the field is invalid
     */
    private Long parseLongField(CSVRecord csvRecord, String fieldName, int lineNumber) 
            throws ParseException {
        String value = getRequiredField(csvRecord, fieldName, lineNumber);
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw ParseException.builder()
                    .message("Invalid reference number at line " + lineNumber)
                    .lineNumber(lineNumber)
                    .fieldName(fieldName)
                    .build();
        }
    }
    
    /**
     * Parses a BigDecimal field from a CSV record.
     *
     * @param csvRecord the CSV record
     * @param fieldName the field name
     * @param lineNumber the line number for error reporting
     * @return the parsed BigDecimal value
     * @throws ParseException if the field is invalid
     */
    private BigDecimal parseBigDecimalField(CSVRecord csvRecord, String fieldName, int lineNumber) 
            throws ParseException {
        String value = getRequiredField(csvRecord, fieldName, lineNumber);
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw ParseException.builder()
                    .message("Invalid " + fieldName.toLowerCase() + " at line " + lineNumber)
                    .lineNumber(lineNumber)
                    .fieldName(fieldName)
                    .build();
        }
    }
    
    /**
     * Parses the mutation field from a CSV record, handling the optional + prefix.
     *
     * @param csvRecord the CSV record
     * @param lineNumber the line number for error reporting
     * @return the parsed BigDecimal value
     * @throws ParseException if the field is invalid
     */
    private BigDecimal parseMutationField(CSVRecord csvRecord, int lineNumber) 
            throws ParseException {
        String value = getRequiredField(csvRecord, "Mutation", lineNumber).trim();
        try {
            // Remove leading + sign if present
            if (value.startsWith("+")) {
                value = value.substring(1);
            }
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw ParseException.builder()
                    .message("Invalid mutation amount at line " + lineNumber)
                    .lineNumber(lineNumber)
                    .fieldName("Mutation")
                    .build();
        }
    }
    
    /**
     * Gets a required field from a CSV record.
     * Handles flexible header names (e.g., "Account Number" or "AccountNumber").
     *
     * @param csvRecord the CSV record
     * @param fieldName the field name
     * @param lineNumber the line number for error reporting
     * @return the field value
     * @throws ParseException if the field is missing or empty
     */
    private String getRequiredField(CSVRecord csvRecord, String fieldName, int lineNumber) 
            throws ParseException {
        String value = null;
        
        // Try to get the field with the exact name first
        try {
            if (csvRecord.isMapped(fieldName)) {
                value = csvRecord.get(fieldName);
            }
        } catch (IllegalArgumentException e) {
            // Field not found with exact name
        }
        
        // If not found, try to find a matching header with normalized names
        if (value == null) {
            String normalizedFieldName = normalizeHeaderName(fieldName);
            for (String header : csvRecord.getParser().getHeaderNames()) {
                if (normalizeHeaderName(header).equals(normalizedFieldName)) {
                    value = csvRecord.get(header);
                    break;
                }
            }
        }
        
        if (value == null) {
            throw ParseException.builder()
                    .message("Missing required field '" + fieldName + "' at line " + lineNumber)
                    .lineNumber(lineNumber)
                    .fieldName(fieldName)
                    .build();
        }
        
        if (value.trim().isEmpty()) {
            throw ParseException.builder()
                    .message("Empty required field '" + fieldName + "' at line " + lineNumber)
                    .lineNumber(lineNumber)
                    .fieldName(fieldName)
                    .build();
        }
        
        return value;
    }
    
    /**
     * Parses a JSON file into transaction records.
     *
     * @param inputStream the input stream to read from
     * @return list of parsed transaction records
     * @throws ParseException if the JSON cannot be parsed (includes IO errors)
     */
    private List<TransactionRecord> parseJson(InputStream inputStream) 
            throws ParseException {
        log.debug("Starting JSON parsing");
        
        try {
            // Parse JSON array
            JsonNode rootNode = objectMapper.readTree(inputStream);
            
            if (!rootNode.isArray()) {
                throw ParseException.builder()
                        .message("JSON root element must be an array")
                        .build();
            }
            
            List<TransactionRecord> records = new ArrayList<>();
            int index = 0;
            
            for (JsonNode node : rootNode) {
                try {
                    TransactionRecord record = parseJsonRecord(node, index);
                    records.add(record);
                    index++;
                } catch (ParseException e) {
                    throw ParseException.builder()
                            .message(e.getMessage())
                            .lineNumber(index)
                            .fieldName(e.getFieldName())
                            .build();
                }
            }
            
            log.debug("Parsed {} records from JSON", records.size());
            return records;
            
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.warn("JSON syntax error: {}", e.getMessage());
            throw ParseException.builder()
                    .message("Malformed JSON: " + e.getMessage())
                    .build();
        } catch (IOException e) {
            log.error("I/O error while parsing JSON", e);
            throw new ParseException("Failed to read JSON file", e);
        }
    }
    
    /**
     * Parses a single JSON node into a TransactionRecord.
     *
     * @param node the JSON node
     * @param index the record index for error reporting
     * @return the parsed transaction record
     * @throws ParseException if required fields are missing or invalid
     */
    private TransactionRecord parseJsonRecord(JsonNode node, int index) throws ParseException {
        Long reference = requiredLong(node, "reference", index);
        String accountNumber = requiredText(node, "accountNumber", index);
        String description = requiredText(node, "description", index);
        BigDecimal startBalance = requiredBigDecimal(node, "startBalance", index);
        BigDecimal mutation = requiredBigDecimal(node, "mutation", index);
        BigDecimal endBalance = requiredBigDecimal(node, "endBalance", index);
        
        return TransactionRecord.builder()
                .reference(reference)
                .accountNumber(accountNumber)
                .description(description)
                .startBalance(startBalance)
                .mutation(mutation)
                .endBalance(endBalance)
                .build();
    }
    
    /**
     * Extracts and validates a required text field from a JSON node.
     *
     * @param node the JSON node
     * @param fieldName the field name
     * @param recordIndex the record index for error reporting
     * @return the trimmed text value
     * @throws ParseException if the field is missing, null, or empty
     */
    private String requiredText(JsonNode node, String fieldName, int recordIndex) throws ParseException {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().trim().isEmpty()) {
            throw ParseException.builder()
                    .message("Missing required field '" + fieldName + "' at record " + recordIndex)
                    .lineNumber(recordIndex)
                    .fieldName(fieldName)
                    .build();
        }
        return fieldNode.asText().trim();
    }
    
    /**
     * Extracts and validates a required Long field from a JSON node.
     *
     * @param node the JSON node
     * @param fieldName the field name
     * @param recordIndex the record index for error reporting
     * @return the Long value
     * @throws ParseException if the field is missing, null, or invalid
     */
    private Long requiredLong(JsonNode node, String fieldName, int recordIndex) throws ParseException {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            throw ParseException.builder()
                    .message("Missing required field '" + fieldName + "' at record " + recordIndex)
                    .lineNumber(recordIndex)
                    .fieldName(fieldName)
                    .build();
        }
        try {
            return fieldNode.asLong();
        } catch (Exception e) {
            throw ParseException.builder()
                    .message("Invalid " + fieldName + " at record " + recordIndex)
                    .lineNumber(recordIndex)
                    .fieldName(fieldName)
                    .build();
        }
    }
    
    /**
     * Extracts and validates a required BigDecimal field from a JSON node.
     *
     * @param node the JSON node
     * @param fieldName the field name
     * @param recordIndex the record index for error reporting
     * @return the BigDecimal value
     * @throws ParseException if the field is missing, null, or invalid
     */
    private BigDecimal requiredBigDecimal(JsonNode node, String fieldName, int recordIndex) throws ParseException {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            throw ParseException.builder()
                    .message("Missing required field '" + fieldName + "' at record " + recordIndex)
                    .lineNumber(recordIndex)
                    .fieldName(fieldName)
                    .build();
        }
        try {
            return new BigDecimal(fieldNode.asText());
        } catch (Exception e) {
            throw ParseException.builder()
                    .message("Invalid " + fieldName + " at record " + recordIndex)
                    .lineNumber(recordIndex)
                    .fieldName(fieldName)
                    .build();
        }
    }
}

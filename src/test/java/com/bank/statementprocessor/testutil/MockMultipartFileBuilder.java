package com.bank.statementprocessor.testutil;

import org.springframework.mock.web.MockMultipartFile;

/**
 * Test data builder for MockMultipartFile.
 * Provides a fluent API for creating test files with sensible defaults.
 */
public class MockMultipartFileBuilder {
    
    private String name = "file";
    private String originalFilename = "test.csv";
    private String contentType = "text/csv";
    private byte[] content = "test content".getBytes();
    
    public static MockMultipartFileBuilder aMultipartFile() {
        return new MockMultipartFileBuilder();
    }
    
    public static MockMultipartFileBuilder aCsvFile() {
        return new MockMultipartFileBuilder()
                .withOriginalFilename("test.csv")
                .withContentType("text/csv");
    }
    
    public static MockMultipartFileBuilder aJsonFile() {
        return new MockMultipartFileBuilder()
                .withOriginalFilename("test.json")
                .withContentType("application/json");
    }

    public MockMultipartFileBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public MockMultipartFileBuilder withOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
        return this;
    }
    
    public MockMultipartFileBuilder withContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
    
    public MockMultipartFileBuilder withContent(String content) {
        this.content = content.getBytes();
        return this;
    }

    public MockMultipartFileBuilder withContent(byte[] content) {
        this.content = content;
        return this;
    }

    /**
     * Creates a CSV file with the given content.
     */
    public MockMultipartFileBuilder withCsvContent(String csvContent) {
        return this.withContentType("text/csv")
                .withOriginalFilename("test.csv")
                .withContent(csvContent);
    }

    /**
     * Creates a JSON file with the given content.
     */
    public MockMultipartFileBuilder withJsonContent(String jsonContent) {
        return this.withContentType("application/json")
                .withOriginalFilename("test.json")
                .withContent(jsonContent);
    }

    /**
     * Creates a CSV file with a single valid transaction record.
     */
    public MockMultipartFileBuilder withValidCsvRecord() {
        String csvContent = "Reference,AccountNumber,Description,Start Balance,Mutation,End Balance\n" +
                "194261,NL91RABO0315273637,Test Transaction,100.00,50.00,150.00";
        return withCsvContent(csvContent);
    }

    /**
     * Creates a JSON file with a single valid transaction record.
     */
    public MockMultipartFileBuilder withValidJsonRecord() {
        String jsonContent = "[{" +
                "\"reference\": 194261," +
                "\"accountNumber\": \"NL91RABO0315273637\"," +
                "\"description\": \"Test Transaction\"," +
                "\"startBalance\": 100.0," +
                "\"mutation\": 50.0," +
                "\"endBalance\": 150.0" +
                "}]";
        return withJsonContent(jsonContent);
    }
    
    public MockMultipartFile build() {
        return new MockMultipartFile(name, originalFilename, contentType, content);
    }
}

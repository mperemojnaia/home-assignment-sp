package com.bank.statementprocessor.testutil;

import java.util.ArrayList;
import java.util.List;

/**
 * Test data builder for CSV content.
 * Provides a fluent API for creating CSV test data.
 */
public class CsvContentBuilder {
    
    private static final String DEFAULT_HEADERS = 
            "Reference,AccountNumber,Description,Start Balance,Mutation,End Balance";
    
    private String headers = DEFAULT_HEADERS;
    private final List<String> rows = new ArrayList<>();
    
    public static CsvContentBuilder aCsvContent() {
        return new CsvContentBuilder();
    }

    public CsvContentBuilder withHeaders(String headers) {
        this.headers = headers;
        return this;
    }

    public CsvContentBuilder withCustomHeaders(String... headerNames) {
        this.headers = String.join(",", headerNames);
        return this;
    }
    
    public CsvContentBuilder withRow(String reference, String accountNumber, String description,
                                     String startBalance, String mutation, String endBalance) {
        rows.add(String.join(",", reference, accountNumber, description, 
                startBalance, mutation, endBalance));
        return this;
    }

    public CsvContentBuilder withRow(Long reference, String accountNumber, String description,
                                     String startBalance, String mutation, String endBalance) {
        return withRow(reference.toString(), accountNumber, description,
                startBalance, mutation, endBalance);
    }

    public CsvContentBuilder withValidRow() {
        return withRow("194261", "NL91RABO0315273637", "Test Transaction",
                "100.00", "50.00", "150.00");
    }
    
    public CsvContentBuilder withInvalidReferenceRow() {
        return withRow("INVALID", "NL91RABO0315273637", "Test Transaction", 
                "100.00", "50.00", "150.00");
    }
    
    public CsvContentBuilder withEmptyFieldRow(String emptyField) {
        switch (emptyField.toLowerCase()) {
            case "reference":
                return withRow("", "NL91RABO0315273637", "Test", "100.00", "50.00", "150.00");
            case "accountnumber":
                return withRow("194261", "", "Test", "100.00", "50.00", "150.00");
            case "description":
                return withRow("194261", "NL91RABO0315273637", "", "100.00", "50.00", "150.00");
            case "startbalance":
                return withRow("194261", "NL91RABO0315273637", "Test", "", "50.00", "150.00");
            case "mutation":
                return withRow("194261", "NL91RABO0315273637", "Test", "100.00", "", "150.00");
            case "endbalance":
                return withRow("194261", "NL91RABO0315273637", "Test", "100.00", "50.00", "");
            default:
                throw new IllegalArgumentException("Unknown field: " + emptyField);
        }
    }

    public CsvContentBuilder withInvalidNumberRow(String invalidField) {
        switch (invalidField.toLowerCase()) {
            case "startbalance":
                return withRow("194261", "NL91RABO0315273637", "Test", "INVALID", "50.00", "150.00");
            case "mutation":
                return withRow("194261", "NL91RABO0315273637", "Test", "100.00", "INVALID", "150.00");
            case "endbalance":
                return withRow("194261", "NL91RABO0315273637", "Test", "100.00", "50.00", "INVALID");
            default:
                throw new IllegalArgumentException("Unknown field: " + invalidField);
        }
    }
    
    public String build() {
        StringBuilder csv = new StringBuilder();
        csv.append(headers).append("\n");
        for (String row : rows) {
            csv.append(row).append("\n");
        }
        return csv.toString();
    }

    public String buildWithoutHeaders() {
        StringBuilder csv = new StringBuilder();
        for (String row : rows) {
            csv.append(row).append("\n");
        }
        return csv.toString();
    }
}

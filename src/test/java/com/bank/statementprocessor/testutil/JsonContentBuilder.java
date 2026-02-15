package com.bank.statementprocessor.testutil;

import java.util.ArrayList;
import java.util.List;

/**
 * Test data builder for JSON content.
 * Provides a fluent API for creating JSON test data.
 */
public class JsonContentBuilder {
    
    private final List<String> records = new ArrayList<>();
    
    public static JsonContentBuilder aJsonContent() {
        return new JsonContentBuilder();
    }
    
    public JsonContentBuilder withRecord(Long reference, String accountNumber, String description,
                                        String startBalance, String mutation, String endBalance) {
        String record = String.format(
                "{\"reference\": %d, \"accountNumber\": \"%s\", \"description\": \"%s\", " +
                "\"startBalance\": %s, \"mutation\": %s, \"endBalance\": %s}",
                reference, accountNumber, description, startBalance, mutation, endBalance
        );
        records.add(record);
        return this;
    }
    
    public JsonContentBuilder withValidRecord() {
        return withRecord(194261L, "NL91RABO0315273637", "Test Transaction",
                "100.0", "50.0", "150.0");
    }

    public JsonContentBuilder withNullField(String fieldName) {
        String record = switch (fieldName.toLowerCase()) {
            case "reference" ->
                "{\"reference\": null, \"accountNumber\": \"NL123\", \"description\": \"Test\", " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": 150.0}";
            case "accountnumber" ->
                "{\"reference\": 123, \"accountNumber\": null, \"description\": \"Test\", " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": 150.0}";
            case "description" ->
                "{\"reference\": 123, \"accountNumber\": \"NL123\", \"description\": null, " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": 150.0}";
            case "startbalance" ->
                "{\"reference\": 123, \"accountNumber\": \"NL123\", \"description\": \"Test\", " +
                "\"startBalance\": null, \"mutation\": 50.0, \"endBalance\": 150.0}";
            case "mutation" ->
                "{\"reference\": 123, \"accountNumber\": \"NL123\", \"description\": \"Test\", " +
                "\"startBalance\": 100.0, \"mutation\": null, \"endBalance\": 150.0}";
            case "endbalance" ->
                "{\"reference\": 123, \"accountNumber\": \"NL123\", \"description\": \"Test\", " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": null}";
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };
        records.add(record);
        return this;
    }

    public JsonContentBuilder withEmptyField(String fieldName) {
        String record = switch (fieldName.toLowerCase()) {
            case "accountnumber" ->
                "{\"reference\": 123, \"accountNumber\": \"\", \"description\": \"Test\", " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": 150.0}";
            case "description" ->
                "{\"reference\": 123, \"accountNumber\": \"NL123\", \"description\": \"\", " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": 150.0}";
            default -> throw new IllegalArgumentException("Cannot create empty field for: " + fieldName);
        };
        records.add(record);
        return this;
    }

    public JsonContentBuilder withInvalidNumberField(String fieldName) {
        String record = switch (fieldName.toLowerCase()) {
            case "startbalance" ->
                "{\"reference\": 123, \"accountNumber\": \"NL123\", \"description\": \"Test\", " +
                "\"startBalance\": \"INVALID\", \"mutation\": 50.0, \"endBalance\": 150.0}";
            case "mutation" ->
                "{\"reference\": 123, \"accountNumber\": \"NL123\", \"description\": \"Test\", " +
                "\"startBalance\": 100.0, \"mutation\": \"INVALID\", \"endBalance\": 150.0}";
            case "endbalance" ->
                "{\"reference\": 123, \"accountNumber\": \"NL123\", \"description\": \"Test\", " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": \"INVALID\"}";
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };
        records.add(record);
        return this;
    }
    
    public JsonContentBuilder withMissingField(String fieldName) {
        String record = switch (fieldName.toLowerCase()) {
            case "reference" -> 
                "{\"accountNumber\": \"NL123\", \"description\": \"Test\", " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": 150.0}";
            case "accountnumber" -> 
                "{\"reference\": 123, \"description\": \"Test\", " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": 150.0}";
            case "description" -> 
                "{\"reference\": 123, \"accountNumber\": \"NL123\", " +
                "\"startBalance\": 100.0, \"mutation\": 50.0, \"endBalance\": 150.0}";
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };
        records.add(record);
        return this;
    }
    
    public String build() {
        return "[" + String.join(",", records) + "]";
    }
    
    public String buildAsObject() {
        if (records.isEmpty()) {
            return "{}";
        }
        return records.get(0);
    }
}

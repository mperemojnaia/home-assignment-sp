# Design Document: Customer Statement Processor

## Overview

The Customer Statement Processor is a Java Spring Boot REST API service that validates bank transaction records from uploaded CSV or JSON files. The system follows a layered architecture with clear separation between API, service, and data access concerns. The validation engine applies two core rules: transaction reference uniqueness and balance calculation correctness. The service returns a structured JSON report containing all validation failures.

The application uses Spring Boot 3.x with Spring Web for REST endpoints, Jackson for JSON processing, Apache Commons CSV for CSV parsing, Lombok for reducing boilerplate code, and SLF4J for logging. The design emphasizes testability through dependency injection and clear interface boundaries.

## Architecture

The system follows a three-layer architecture:

```
┌─────────────────────────────────────────────────────────┐
│                     API Layer                            │
│  - StatementController (REST endpoints)                  │
│  - Request/Response DTOs                                 │
│  - Exception Handlers                                    │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                   Service Layer                          │
│  - StatementValidationService                            │
│  - FileParserService (CSV/JSON)                          │
│  - ValidationRuleEngine                                  │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                   Domain Layer                           │
│  - TransactionRecord (entity)                            │
│  - ValidationReport (entity)                             │
│  - ValidationFailure (entity)                            │
└─────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

**API Layer:**
- Handles HTTP requests and responses
- Validates multipart file uploads
- Maps domain objects to DTOs
- Handles exceptions and returns appropriate HTTP status codes
- Provides OpenAPI documentation

**Service Layer:**
- Orchestrates validation workflow
- Delegates file parsing to appropriate parser
- Applies validation rules to transaction records
- Constructs validation reports

**Domain Layer:**
- Defines core business entities
- Encapsulates business logic and validation rules
- Provides immutable data structures

## Components and Interfaces

### 1. StatementController

REST controller exposing the validation endpoint.

```java
@RestController
@RequestMapping("/api/v1/statements")
@RequiredArgsConstructor
@Slf4j
public class StatementController {
    
    private final StatementValidationService validationService;
    
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ValidationReportDTO> validateStatement(
        @RequestParam("file") MultipartFile file
    ) throws IOException;
}
```

**Responsibilities:**
- Accept multipart file uploads
- Validate file presence and size
- Delegate to validation service
- Map ValidationReport to ValidationReportDTO
- Handle exceptions and return appropriate HTTP responses
- Log all requests and errors using SLF4J

### 2. StatementValidationService

Core service orchestrating the validation workflow.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StatementValidationService {
    
    private final FileParserService fileParserService;
    private final ValidationRuleEngine validationRuleEngine;
    
    public ValidationReport validateStatement(MultipartFile file) throws IOException;
}
```

**Workflow:**
1. Determine file format (CSV or JSON)
2. Parse file into List<TransactionRecord>
3. Apply validation rules
4. Construct and return ValidationReport

### 3. FileParserService

Service responsible for parsing files into transaction records.

```java
@Service
@Slf4j
public class FileParserService {
    
    public List<TransactionRecord> parseFile(
        MultipartFile file, 
        FileFormat format
    ) throws IOException, ParseException;
    
    public FileFormat detectFormat(MultipartFile file);
    
    private List<TransactionRecord> parseCsv(InputStream inputStream) throws IOException;
    
    private List<TransactionRecord> parseJson(InputStream inputStream) throws IOException;
}
```

**CSV Parsing:**
- Uses Apache Commons CSV library
- Expects header row: Reference,Account Number,Description,Start Balance,Mutation,End Balance
- Handles quoted fields and embedded commas
- Parses numeric fields with BigDecimal for precision

**JSON Parsing:**
- Uses Jackson ObjectMapper
- Expects array of transaction objects
- Maps JSON fields to TransactionRecord properties
- Validates required fields are present

### 4. ValidationRuleEngine

Engine that applies validation rules to transaction records.

```java
@Component
@Slf4j
public class ValidationRuleEngine {
    
    public ValidationReport validate(List<TransactionRecord> records);
    
    private Set<Long> findDuplicateReferences(List<TransactionRecord> records);
    
    private boolean isBalanceCorrect(TransactionRecord record);
}
```

**Validation Rules:**

1. **Reference Uniqueness:**
   - Collect all references into a Set
   - Identify references that appear more than once
   - Mark all instances of duplicate references as failed

2. **Balance Calculation:**
   - For each record: verify startBalance + mutation = endBalance
   - Use BigDecimal arithmetic for precision
   - Apply tolerance of 0.01 for floating-point comparison
   - Mark records with incorrect calculations as failed

### 5. Domain Models

**TransactionRecord:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord {
    private Long reference;
    private String accountNumber;
    private String description;
    private BigDecimal startBalance;
    private BigDecimal mutation;
    private BigDecimal endBalance;
    
    public boolean isBalanceCorrect() {
        BigDecimal calculated = startBalance.add(mutation);
        BigDecimal difference = calculated.subtract(endBalance).abs();
        return difference.compareTo(new BigDecimal("0.01")) <= 0;
    }
}
```

**ValidationFailure:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationFailure {
    private Long reference;
    private String description;
    private Set<FailureReason> reasons;
    
    public enum FailureReason {
        DUPLICATE_REFERENCE,
        INCORRECT_END_BALANCE
    }
}
```

**ValidationReport:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReport {
    private boolean valid;
    private List<ValidationFailure> failures;
    private int totalRecords;
    private int failedRecords;
}
```

## Data Models

### Input Data Formats

**CSV Format:**
```csv
Reference,Account Number,Description,Start Balance,Mutation,End Balance
194261,NL91RABO0315273637,Clothes from Jan Bakker,21.6,-41.83,-20.23
112806,NL27SNSB0917829871,Clothes from Richard de Vries,91.23,+15.57,106.8
112806,NL69ABNA0433647324,Clothes for Willem Dekker,86.66,+44.5,131.16
```

**JSON Format:**
```json
[
  {
    "reference": 194261,
    "accountNumber": "NL91RABO0315273637",
    "description": "Clothes from Jan Bakker",
    "startBalance": 21.6,
    "mutation": -41.83,
    "endBalance": -20.23
  },
  {
    "reference": 112806,
    "accountNumber": "NL27SNSB0917829871",
    "description": "Clothes from Richard de Vries",
    "startBalance": 91.23,
    "mutation": 15.57,
    "endBalance": 106.8
  }
]
```

### Output Data Format

**ValidationReportDTO:**
```json
{
  "valid": false,
  "totalRecords": 10,
  "failedRecords": 3,
  "failures": [
    {
      "reference": 112806,
      "description": "Clothes from Richard de Vries",
      "reasons": ["DUPLICATE_REFERENCE"]
    },
    {
      "reference": 112806,
      "description": "Clothes for Willem Dekker",
      "reasons": ["DUPLICATE_REFERENCE"]
    },
    {
      "reference": 194261,
      "description": "Clothes from Jan Bakker",
      "reasons": ["INCORRECT_END_BALANCE"]
    }
  ]
}
```

### OpenAPI 3.0 Specification

```yaml
openapi: 3.0.3
info:
  title: Customer Statement Processor API
  description: REST API for validating bank transaction records from CSV or JSON files
  version: 1.0.0
  contact:
    name: API Support
    email: support@example.com

servers:
  - url: http://localhost:8080
    description: Local development server

paths:
  /api/v1/statements/validate:
    post:
      summary: Validate transaction statement file
      description: Upload a CSV or JSON file containing transaction records for validation
      operationId: validateStatement
      tags:
        - Statement Validation
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary
                  description: CSV or JSON file containing transaction records
              required:
                - file
      responses:
        '200':
          description: Validation completed successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ValidationReport'
              examples:
                withFailures:
                  summary: Report with validation failures
                  value:
                    valid: false
                    totalRecords: 10
                    failedRecords: 3
                    failures:
                      - reference: 112806
                        description: "Clothes from Richard de Vries"
                        reasons: ["DUPLICATE_REFERENCE"]
                      - reference: 112806
                        description: "Clothes for Willem Dekker"
                        reasons: ["DUPLICATE_REFERENCE"]
                      - reference: 194261
                        description: "Clothes from Jan Bakker"
                        reasons: ["INCORRECT_END_BALANCE"]
                allValid:
                  summary: All records valid
                  value:
                    valid: true
                    totalRecords: 10
                    failedRecords: 0
                    failures: []
        '400':
          description: Bad request - file missing or malformed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                missingFile:
                  summary: No file provided
                  value:
                    error: "File is required"
                    timestamp: "2024-01-15T10:30:00Z"
                malformedFile:
                  summary: File cannot be parsed
                  value:
                    error: "Failed to parse file: Invalid CSV format at line 5"
                    timestamp: "2024-01-15T10:30:00Z"
        '413':
          description: File too large
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              example:
                error: "File too large. Maximum size is 10MB"
                timestamp: "2024-01-15T10:30:00Z"
        '415':
          description: Unsupported file format
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              example:
                error: "Unsupported file format. Only CSV and JSON are supported"
                timestamp: "2024-01-15T10:30:00Z"
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              example:
                error: "An unexpected error occurred"
                timestamp: "2024-01-15T10:30:00Z"

components:
  schemas:
    ValidationReport:
      type: object
      required:
        - valid
        - totalRecords
        - failedRecords
        - failures
      properties:
        valid:
          type: boolean
          description: Whether all records passed validation
        totalRecords:
          type: integer
          description: Total number of records processed
          minimum: 0
        failedRecords:
          type: integer
          description: Number of records that failed validation
          minimum: 0
        failures:
          type: array
          description: List of validation failures
          items:
            $ref: '#/components/schemas/ValidationFailure'
    
    ValidationFailure:
      type: object
      required:
        - reference
        - description
        - reasons
      properties:
        reference:
          type: integer
          format: int64
          description: Transaction reference number
        description:
          type: string
          description: Transaction description
        reasons:
          type: array
          description: List of validation failure reasons
          items:
            type: string
            enum:
              - DUPLICATE_REFERENCE
              - INCORRECT_END_BALANCE
    
    ErrorResponse:
      type: object
      required:
        - error
        - timestamp
      properties:
        error:
          type: string
          description: Error message
        timestamp:
          type: string
          format: date-time
          description: Timestamp when error occurred
```



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: CSV Parsing Round-Trip

*For any* valid list of TransactionRecord objects, serializing them to CSV format and then parsing the CSV back should produce an equivalent list of TransactionRecord objects with all field values preserved.

**Validates: Requirements 1.1**

### Property 2: JSON Parsing Round-Trip

*For any* valid list of TransactionRecord objects, serializing them to JSON format and then parsing the JSON back should produce an equivalent list of TransactionRecord objects with all field values preserved.

**Validates: Requirements 1.2**

### Property 3: Malformed Data Error Handling

*For any* file with malformed data (invalid CSV structure, invalid JSON syntax, or missing required fields), the File_Parser should return a descriptive error rather than crashing or producing incorrect TransactionRecord objects.

**Validates: Requirements 1.3**

### Property 4: Duplicate Reference Detection

*For any* list of TransactionRecord objects containing duplicate references, the Validation_Service should identify all instances of each duplicate reference and mark them as failed with reason DUPLICATE_REFERENCE in the ValidationReport.

**Validates: Requirements 2.1, 2.2**

### Property 5: Failure Report Structure

*For any* ValidationFailure in a ValidationReport, the failure should include both the transaction reference number and the transaction description.

**Validates: Requirements 2.3**

### Property 6: Balance Calculation Validation

*For any* TransactionRecord, the Validation_Service should mark it as failed with reason INCORRECT_END_BALANCE if and only if the absolute difference between (startBalance + mutation) and endBalance exceeds 0.01.

**Validates: Requirements 3.1, 3.2, 3.3**

### Property 7: Validation Report Completeness

*For any* list of TransactionRecord objects, the ValidationReport should contain exactly those records that fail validation rules (duplicate reference or incorrect balance), with each failure correctly categorized by its failure reasons.

**Validates: Requirements 4.1, 4.4**

### Property 8: Validation Report JSON Serialization

*For any* ValidationReport object, serializing it to JSON and then deserializing back should produce an equivalent ValidationReport with all fields preserved.

**Validates: Requirements 4.6**

### Property 9: Required Fields Validation

*For any* transaction record missing one or more required fields (reference, accountNumber, startBalance, mutation, endBalance), the File_Parser should reject the record and return an error indicating which fields are missing.

**Validates: Requirements 6.5**

### Property 10: File Format Detection

*For any* file with a .csv extension or text/csv content type, the File_Parser should detect it as CSV format; for any file with a .json extension or application/json content type, the File_Parser should detect it as JSON format.

**Validates: Requirements 7.1**

## Error Handling

The system implements comprehensive error handling at multiple layers:

### API Layer Error Handling

**File Validation Errors:**
- Missing file: HTTP 400 with message "File is required"
- File too large: HTTP 413 with message "File too large. Maximum size is 10MB"
- Unsupported format: HTTP 415 with message "Unsupported file format. Only CSV and JSON are supported"

**Parsing Errors:**
- Malformed CSV: HTTP 400 with message "Failed to parse file: Invalid CSV format at line X"
- Malformed JSON: HTTP 400 with message "Failed to parse file: Invalid JSON syntax"
- Missing required fields: HTTP 400 with message "Failed to parse file: Missing required field 'fieldName' at record X"

**System Errors:**
- Unexpected exceptions: HTTP 500 with message "An unexpected error occurred"
- All errors logged with stack traces using SLF4J for debugging

### Service Layer Error Handling

**FileParserService:**
- Wraps IOException with custom ParseException containing context
- Validates file format before parsing
- Validates required fields during parsing
- Provides detailed error messages with line/record numbers

**ValidationRuleEngine:**
- Handles null or empty record lists gracefully
- Handles null field values by treating as validation failures
- Uses BigDecimal to avoid floating-point arithmetic errors

### Exception Hierarchy

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseException extends Exception {
    private final int lineNumber;
    private final String fieldName;
}

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ErrorResponse> handleParseException(ParseException ex);
    
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex);
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex);
}
```

## Testing Strategy

The testing strategy employs a dual approach combining unit tests for specific scenarios and property-based tests for comprehensive validation of universal properties.

### Property-Based Testing

Property-based tests verify that correctness properties hold across a wide range of randomly generated inputs. We will use **JUnit-Quickcheck** as the property-based testing library for Java.

**Configuration:**
- Minimum 100 iterations per property test
- Each test tagged with: **Feature: customer-statement-processor, Property N: [property text]**
- Random data generators for TransactionRecord, ValidationReport, and file content

**Property Test Coverage:**

1. **CSV Parsing Round-Trip** (Property 1)
   - Generate random TransactionRecord lists
   - Serialize to CSV, parse back, verify equivalence
   - Test with various field values: positive/negative numbers, special characters in descriptions, large numbers

2. **JSON Parsing Round-Trip** (Property 2)
   - Generate random TransactionRecord lists
   - Serialize to JSON, parse back, verify equivalence
   - Test with various field values and edge cases

3. **Malformed Data Error Handling** (Property 3)
   - Generate malformed CSV files (missing columns, invalid numbers, etc.)
   - Generate malformed JSON files (invalid syntax, wrong types, etc.)
   - Verify all produce appropriate errors

4. **Duplicate Reference Detection** (Property 4)
   - Generate lists with known duplicate references
   - Verify all duplicates are identified and marked as failed
   - Verify non-duplicates are not marked as failed

5. **Balance Calculation Validation** (Property 6)
   - Generate records with correct and incorrect balances
   - Generate records at the tolerance boundary (exactly 0.01 difference)
   - Verify validation correctly identifies failures

6. **Validation Report Completeness** (Property 7)
   - Generate mixed lists with various validation failures
   - Verify report contains exactly the failed records
   - Verify failure reasons are correctly categorized

### Unit Testing

Unit tests focus on specific examples, edge cases, and integration points.

**Test Coverage:**

1. **FileParserService Tests:**
   - Parse valid CSV file with sample data
   - Parse valid JSON file with sample data
   - Handle empty files
   - Handle files with only headers (CSV)
   - Handle empty arrays (JSON)
   - Reject files with missing required fields
   - Reject files with invalid number formats

2. **ValidationRuleEngine Tests:**
   - Validate records with correct balances
   - Identify records with incorrect balances
   - Identify duplicate references (including the sample data case with reference 112806)
   - Handle records with both duplicate reference and incorrect balance
   - Handle empty record lists
   - Handle lists with all valid records

3. **StatementController Integration Tests:**
   - Upload valid CSV file, receive validation report
   - Upload valid JSON file, receive validation report
   - Upload file with validation failures, verify failures in response
   - Upload without file parameter, receive HTTP 400
   - Upload unsupported format, receive HTTP 415
   - Upload file exceeding size limit, receive HTTP 413

4. **Edge Cases:**
   - Very large files (performance testing)
   - Records with zero balances
   - Records with very large numbers
   - Records with maximum precision decimals
   - Special characters in descriptions (quotes, commas, newlines)
   - Unicode characters in descriptions

### Test Data

Use the provided sample files in `test-data/` for integration testing:
- **test-data/records.csv**: Contains duplicate reference 112806 (appears 3 times)
- **test-data/records.json**: Contains sample transaction data
- **test-data/records-large.json**: Large file with 200 records for performance testing

### Testing Tools

- **JUnit 5**: Unit testing framework
- **JUnit-Quickcheck**: Property-based testing library
- **MockMvc**: Spring MVC testing support
- **AssertJ**: Fluent assertions
- **Mockito**: Mocking framework for unit tests

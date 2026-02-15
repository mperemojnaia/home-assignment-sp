# Requirements Document

## Introduction

The Customer Statement Processor is a Java Spring Boot validation service that processes bank transaction records from uploaded files (CSV or JSON format) and returns a comprehensive validation report identifying any records that fail validation rules. The system ensures data integrity by validating transaction reference uniqueness and mathematical correctness of balance calculations.

## Glossary

- **Transaction_Record**: A single bank transaction entry containing reference, account number, description, start balance, mutation amount, and end balance
- **Validation_Service**: The core component that applies validation rules to transaction records
- **File_Parser**: Component responsible for parsing CSV and JSON file formats into Transaction_Record objects
- **Validation_Report**: The output document containing all failed transaction records with failure reasons
- **Reference**: A unique numeric identifier for each transaction
- **IBAN**: International Bank Account Number format for account identification
- **Mutation**: The transaction amount (positive for credits, negative for debits)
- **Balance_Calculation**: The mathematical operation: startBalance + mutation = endBalance

## Requirements

### Requirement 1: File Upload and Processing

**User Story:** As a bank operations user, I want to upload transaction files in CSV or JSON format, so that I can validate multiple transaction records efficiently.

#### Acceptance Criteria

1. WHEN a user uploads a CSV file, THE File_Parser SHALL parse it into Transaction_Record objects
2. WHEN a user uploads a JSON file, THE File_Parser SHALL parse it into Transaction_Record objects
3. WHEN a file contains malformed data, THE File_Parser SHALL return a descriptive error message
4. THE File_Parser SHALL extract reference, accountNumber, description, startBalance, mutation, and endBalance fields from each record
5. WHEN parsing is complete, THE System SHALL pass all Transaction_Record objects to the Validation_Service

### Requirement 2: Transaction Reference Uniqueness

**User Story:** As a bank operations user, I want to ensure all transaction references are unique, so that I can identify duplicate or erroneous entries.

#### Acceptance Criteria

1. THE Validation_Service SHALL check that all transaction references are unique across all records in a file
2. WHEN duplicate references are detected, THE Validation_Service SHALL mark all instances as failed with reason "duplicate reference"
3. THE Validation_Service SHALL include the reference number and description in the failure report for each duplicate

### Requirement 3: Balance Calculation Validation

**User Story:** As a bank operations user, I want to verify that balance calculations are mathematically correct, so that I can identify calculation errors or data corruption.

#### Acceptance Criteria

1. THE Validation_Service SHALL verify that startBalance + mutation equals endBalance for each Transaction_Record
2. WHEN calculating balance equality, THE Validation_Service SHALL handle floating-point precision with tolerance of 0.01
3. WHEN a balance calculation is incorrect, THE Validation_Service SHALL mark the record as failed with reason "incorrect end balance"
4. THE Validation_Service SHALL include the reference number and description in the failure report for each incorrect balance

### Requirement 4: Validation Report Generation

**User Story:** As a bank operations user, I want to receive a structured validation report, so that I can quickly identify and address problematic records.

#### Acceptance Criteria

1. THE Validation_Report SHALL contain all Transaction_Record objects that failed validation
2. THE Validation_Report SHALL include the transaction reference for each failed record
3. THE Validation_Report SHALL include the description for each failed record
4. THE Validation_Report SHALL indicate the failure type (duplicate reference, incorrect balance, or both)
5. WHEN all records pass validation, THE Validation_Report SHALL indicate success with an empty failure list
6. THE System SHALL serialize the Validation_Report as JSON

### Requirement 5: RESTful API Design

**User Story:** As an API consumer, I want a well-defined RESTful endpoint for file upload, so that I can integrate the validation service into existing systems.

#### Acceptance Criteria

1. THE API SHALL provide a POST endpoint for file upload at /api/v1/statements/validate
2. THE API SHALL accept multipart/form-data requests with a file parameter
3. THE API SHALL return HTTP 200 with the Validation_Report when processing succeeds
4. WHEN a file is malformed or cannot be parsed, THE API SHALL return HTTP 400 with an error message
5. WHEN an unsupported file format is uploaded, THE API SHALL return HTTP 415 with an error message
6. THE API SHALL include an OpenAPI 3.0 specification document accessible at /api-docs

### Requirement 6: Input Validation and Error Handling

**User Story:** As a system administrator, I want robust error handling and input validation, so that the service remains stable and provides clear feedback.

#### Acceptance Criteria

1. WHEN no file is provided in the request, THE API SHALL return HTTP 400 with message "File is required"
2. WHEN a file exceeds the maximum size limit, THE API SHALL return HTTP 413 with message "File too large"
3. WHEN an unexpected error occurs during processing, THE API SHALL return HTTP 500 with a generic error message
4. THE System SHALL log all errors with sufficient detail for debugging
5. THE System SHALL validate that required fields (reference, accountNumber, startBalance, mutation, endBalance) are present in each record

### Requirement 7: Data Format Support

**User Story:** As a bank operations user, I want to work with both CSV and JSON formats, so that I can use the format most convenient for my workflow.

#### Acceptance Criteria

1. THE File_Parser SHALL detect file format based on content type or file extension
2. WHEN parsing CSV files, THE File_Parser SHALL handle comma-separated values with optional quoted fields
3. WHEN parsing CSV files, THE File_Parser SHALL treat the first row as a header row
4. WHEN parsing JSON files, THE File_Parser SHALL expect an array of transaction objects
5. THE File_Parser SHALL handle both positive and negative mutation values correctly

### Requirement 8: Testing and Quality Assurance

**User Story:** As a developer, I want comprehensive test coverage, so that I can ensure the validation logic works correctly and prevent regressions.

#### Acceptance Criteria

1. THE System SHALL include unit tests for the Validation_Service covering all validation rules
2. THE System SHALL include unit tests for the File_Parser covering both CSV and JSON formats
3. THE System SHALL include integration tests that upload files and verify the complete validation flow
4. THE System SHALL include property-based tests for balance calculation validation
5. THE System SHALL include property-based tests for reference uniqueness validation

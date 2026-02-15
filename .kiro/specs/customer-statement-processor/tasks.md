# Implementation Plan: Customer Statement Processor

## Overview

This implementation plan breaks down the Customer Statement Processor into discrete coding tasks. The approach follows a bottom-up strategy: first implementing domain models and core validation logic, then adding file parsing capabilities, and finally wiring everything together with the REST API layer. Each task builds incrementally, with property-based tests integrated early to catch errors.

## Tasks

- [x] 1. Set up project structure and dependencies
  - Create Spring Boot 3.x project with Maven/Gradle
  - Add dependencies: spring-boot-starter-web, lombok, slf4j, jackson, commons-csv, junit-quickcheck, assertj, mockito
  - Configure application.properties with file upload limits (max 10MB)
  - Set up logging configuration for SLF4J
  - _Requirements: 5.1, 6.2_

- [ ] 2. Implement domain models
  - [x] 2.1 Create TransactionRecord entity
    - Implement with Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
    - Add fields: reference, accountNumber, description, startBalance, mutation, endBalance
    - Implement isBalanceCorrect() method with BigDecimal arithmetic and 0.01 tolerance
    - _Requirements: 1.4, 3.1, 3.2_
  
  - [x] 2.2 Create ValidationFailure entity
    - Implement with Lombok annotations
    - Add fields: reference, description, reasons (Set<FailureReason>)
    - Define FailureReason enum with DUPLICATE_REFERENCE and INCORRECT_END_BALANCE
    - _Requirements: 2.3, 4.4_
  
  - [x] 2.3 Create ValidationReport entity
    - Implement with Lombok annotations
    - Add fields: valid, failures, totalRecords, failedRecords
    - _Requirements: 4.1, 4.5_

- [ ] 3. Implement validation engine
  - [x] 3.1 Create ValidationRuleEngine component
    - Implement validate() method that applies all validation rules
    - Implement findDuplicateReferences() to identify duplicate transaction references
    - Implement balance validation using TransactionRecord.isBalanceCorrect()
    - Construct ValidationReport with all failures and correct counts
    - Add @Slf4j for logging validation results
    - _Requirements: 2.1, 2.2, 3.1, 3.3, 4.1_
  
  - [x] 3.2 Write property test for duplicate reference detection
    - **Property 4: Duplicate Reference Detection**
    - **Validates: Requirements 2.1, 2.2**
    - Generate lists with known duplicate references
    - Verify all duplicates are identified and marked as failed
    - Configure for minimum 100 iterations
  
  - [x] 3.3 Write property test for balance calculation validation
    - **Property 6: Balance Calculation Validation**
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - Generate records with correct and incorrect balances
    - Test boundary cases at exactly 0.01 tolerance
    - Verify validation correctly identifies failures
    - Configure for minimum 100 iterations
  
  - [x] 3.4 Write property test for validation report completeness
    - **Property 7: Validation Report Completeness**
    - **Validates: Requirements 4.1, 4.4**
    - Generate mixed lists with various validation failures
    - Verify report contains exactly the failed records with correct reasons
    - Configure for minimum 100 iterations
  
  - [x] 3.5 Write unit tests for ValidationRuleEngine
    - Test with sample data (reference 112806 appearing 3 times)
    - Test records with both duplicate reference and incorrect balance
    - Test empty record lists
    - Test lists with all valid records
    - _Requirements: 2.1, 2.2, 3.1, 3.3_

- [x] 4. Checkpoint - Ensure validation tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement file parsing service
  - [x] 5.1 Create FileFormat enum and ParseException class
    - Define FileFormat enum with CSV and JSON values
    - Implement ParseException with Lombok annotations
    - Add fields: lineNumber, fieldName for detailed error reporting
    - _Requirements: 1.3, 6.5_
  
  - [x] 5.2 Create FileParserService
    - Implement detectFormat() method based on content type and file extension
    - Implement parseFile() method that delegates to CSV or JSON parser
    - Add @Slf4j for logging parsing operations
    - _Requirements: 1.1, 1.2, 7.1_
  
  - [x] 5.3 Implement CSV parsing
    - Use Apache Commons CSV library
    - Parse header row and validate expected columns
    - Map each row to TransactionRecord using BigDecimal for numeric fields
    - Handle quoted fields and embedded commas
    - Validate required fields are present
    - Throw ParseException with line numbers for errors
    - _Requirements: 1.1, 1.4, 6.5, 7.2, 7.3_
  
  - [x] 5.4 Implement JSON parsing
    - Use Jackson ObjectMapper
    - Parse JSON array into List<TransactionRecord>
    - Validate required fields are present
    - Throw ParseException for malformed JSON
    - _Requirements: 1.2, 1.4, 6.5, 7.4_
  
  - [x] 5.5 Write property test for CSV parsing round-trip
    - **Property 1: CSV Parsing Round-Trip**
    - **Validates: Requirements 1.1**
    - Generate random TransactionRecord lists
    - Serialize to CSV, parse back, verify equivalence
    - Test with special characters, positive/negative numbers
    - Configure for minimum 100 iterations
  
  - [x] 5.6 Write property test for JSON parsing round-trip
    - **Property 2: JSON Parsing Round-Trip**
    - **Validates: Requirements 1.2**
    - Generate random TransactionRecord lists
    - Serialize to JSON, parse back, verify equivalence
    - Configure for minimum 100 iterations
  
  - [x] 5.7 Write property test for malformed data error handling
    - **Property 3: Malformed Data Error Handling**
    - **Validates: Requirements 1.3**
    - Generate malformed CSV files (missing columns, invalid numbers)
    - Generate malformed JSON files (invalid syntax, wrong types)
    - Verify all produce ParseException with descriptive messages
    - Configure for minimum 100 iterations
  
  - [x] 5.8 Write property test for required fields validation
    - **Property 9: Required Fields Validation**
    - **Validates: Requirements 6.5**
    - Generate records missing various required fields
    - Verify parser rejects them with appropriate error messages
    - Configure for minimum 100 iterations
  
  - [x] 5.9 Write unit tests for FileParserService
    - Test parsing valid CSV file with sample data (records.csv)
    - Test parsing valid JSON file with sample data (records.json)
    - Test empty files and files with only headers
    - Test format detection for various file extensions and content types
    - _Requirements: 1.1, 1.2, 7.1_

- [x] 6. Checkpoint - Ensure parsing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Implement validation service
  - [x] 7.1 Create StatementValidationService
    - Inject FileParserService and ValidationRuleEngine
    - Implement validateStatement() method
    - Detect file format, parse file, apply validation rules
    - Add @Slf4j for logging the validation workflow
    - Handle exceptions and wrap with appropriate context
    - _Requirements: 1.5, 2.1, 3.1_
  
  - [x] 7.2 Write unit tests for StatementValidationService
    - Test complete validation workflow with valid files
    - Test workflow with files containing validation failures
    - Test error handling for parsing failures
    - Mock FileParserService and ValidationRuleEngine
    - _Requirements: 1.5, 2.1, 3.1_

- [ ] 8. Implement REST API layer
  - [x] 8.1 Create ValidationReportDTO and ErrorResponse DTOs
    - Implement ValidationReportDTO matching OpenAPI schema
    - Implement ValidationFailureDTO with reference, description, reasons
    - Implement ErrorResponse with error message and timestamp
    - Use Lombok annotations for all DTOs
    - _Requirements: 4.6, 5.3, 5.4_
  
  - [x] 8.2 Create StatementController
    - Implement POST /api/v1/statements/validate endpoint
    - Accept multipart/form-data with file parameter
    - Validate file presence
    - Delegate to StatementValidationService
    - Map ValidationReport to ValidationReportDTO
    - Add @Slf4j for logging requests
    - _Requirements: 5.1, 5.2, 5.3_
  
  - [x] 8.3 Create GlobalExceptionHandler
    - Handle ParseException → HTTP 400 with error details
    - Handle IOException → HTTP 500 with generic message
    - Handle missing file → HTTP 400 "File is required"
    - Handle file too large → HTTP 413 "File too large"
    - Handle unsupported format → HTTP 415 "Unsupported file format"
    - Handle generic exceptions → HTTP 500
    - Add @Slf4j for logging all exceptions with stack traces
    - _Requirements: 5.4, 5.5, 6.1, 6.2, 6.3, 6.4_
  
  - [x] 8.4 Write property test for validation report JSON serialization
    - **Property 8: Validation Report JSON Serialization**
    - **Validates: Requirements 4.6**
    - Generate random ValidationReport objects
    - Serialize to JSON, deserialize back, verify equivalence
    - Configure for minimum 100 iterations

- [ ] 9. Add OpenAPI documentation
  - [x] 9.1 Configure Springdoc OpenAPI
    - Add springdoc-openapi-starter-webmvc-ui dependency
    - Configure OpenAPI metadata (title, description, version)
    - Ensure /api-docs endpoint is accessible
    - _Requirements: 5.6_
  
  - [x] 9.2 Add OpenAPI annotations to controller
    - Add @Operation, @ApiResponse annotations to validateStatement endpoint
    - Document request body with @RequestBody annotation
    - Document all response codes (200, 400, 413, 415, 500)
    - Add example responses matching the design specification
    - _Requirements: 5.6_

- [x] 10. Integration testing
  - [x] 10.1 Write integration tests for complete API flow
    - Test uploading valid CSV file (records.csv), verify validation report
    - Test uploading valid JSON file (records.json), verify validation report
    - Test uploading file with validation failures, verify failures in response
    - Test uploading without file parameter, verify HTTP 400
    - Test uploading unsupported format, verify HTTP 415
    - Test uploading oversized file, verify HTTP 413
    - Use MockMvc for API testing
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2_
  
  - [x] 10.2 Write edge case tests
    - Test very large files (performance)
    - Test records with zero balances
    - Test records with very large numbers
    - Test special characters in descriptions (quotes, commas, newlines, unicode)
    - _Requirements: 1.1, 1.2, 3.1_

- [x] 11. Final checkpoint - Ensure all tests pass
  - Run complete test suite
  - Verify all property tests run with minimum 100 iterations
  - Verify OpenAPI documentation is accessible at /api-docs
  - Test with provided sample files (records.csv, records.json)
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Each task references specific requirements for traceability
- Property tests validate universal correctness properties with minimum 100 iterations
- Unit tests validate specific examples and edge cases
- Integration tests verify the complete API workflow
- Lombok reduces boilerplate code for all domain models and services
- SLF4J provides consistent logging throughout the application

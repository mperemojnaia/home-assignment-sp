# Customer Statement Processor

A Java Spring Boot REST API service for validating bank transaction records from uploaded CSV or JSON files.

## Overview

The Customer Statement Processor validates transaction records by checking:
- **Reference Uniqueness**: Ensures all transaction references are unique
- **Balance Calculation**: Verifies that `startBalance + mutation = endBalance` (with 0.01 tolerance)

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.1**
- **Maven** for dependency management
- **Lombok** for reducing boilerplate code
- **SLF4J/Logback** for logging
- **Jackson** for JSON processing
- **Apache Commons CSV** for CSV parsing
- **SpringDoc OpenAPI** for API documentation
- **JUnit 5** for unit testing
- **JUnit-Quickcheck** for property-based testing
- **AssertJ** for fluent assertions
- **Mockito** for mocking

## Building the Project

```bash
mvn clean install
```

## Running the Application

### Local Development

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Docker Deployment

#### Quick Start with Docker Compose (Recommended)

```bash
# Build and start the application
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

#### Using the Helper Script

A convenience script is provided for common Docker operations:

```bash
# Make the script executable (first time only)
chmod +x docker-run.sh

# Build the Docker image
./docker-run.sh build

# Start the application
./docker-run.sh start

# Check status and health
./docker-run.sh status

# View logs
./docker-run.sh logs

# Test the API
./docker-run.sh test

# Stop the application
./docker-run.sh stop
```

## API Documentation

Once the application is running, access the OpenAPI documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## API Endpoints

### POST /api/v1/statements/validate

Upload a CSV or JSON file containing transaction records for validation.

**Request:**
- Content-Type: `multipart/form-data`
- Parameter: `file` (CSV or JSON file, max 10MB)

**Response:**
- HTTP 200: Validation report with any failures
- HTTP 400: Bad request (missing file, malformed data)
- HTTP 413: File too large
- HTTP 415: Unsupported file format
- HTTP 500: Internal server error

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ValidationRuleEngineTest

# Run with coverage
mvn test jacoco:report
```

## Configuration

Key configuration properties in `application.properties`:

```properties
# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Server port
server.port=8080

# Logging
logging.level.com.bank.statementprocessor=DEBUG
```

## Sample Data

Sample test files are provided in the `test-data/` directory:
- `test-data/records.csv` - CSV format with sample transactions
- `test-data/records.json` - JSON format with sample transactions
- `test-data/records-large.json` - Large JSON file with 200 records for performance testing

# Transaction Record Validation API - cURL Examples

## Prerequisites
- Start the application: `mvn spring-boot:run` or `./mvnw spring-boot:run`
- Default port: 8080
- Base URL: `http://localhost:8080`

## 1. Valid CSV File (Success Case)

```bash
curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@test-data/records.csv" \
  -H "Accept: application/json"
```

Expected: 200 OK with validation report showing some failures (duplicate references and incorrect balances)

## 2. Valid JSON File (Success Case)

```bash
curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@test-data/records.json" \
  -H "Accept: application/json"
```

Expected: 200 OK with validation report showing some failures

## 3. Missing File Parameter (Error Case - 400 Bad Request)

```bash
curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -H "Accept: application/json"
```

Expected: 400 Bad Request

## 4. Unsupported File Format (Error Case - 415 Unsupported Media Type)

```bash
# Create a text file
echo "This is not a valid format" > invalid.txt

curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@invalid.txt" \
  -H "Accept: application/json"
```

Expected: 415 Unsupported Media Type

## 5. Valid CSV with All Correct Records

```bash
# Create a CSV with all valid records
cat > valid-records.csv << 'EOF'
Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
100001,NL91RABO0315273637,Payment,100.00,50.00,150.00
100002,NL27SNSB0917829871,Salary,200.00,1000.00,1200.00
100003,NL69ABNA0433647324,Groceries,500.00,-50.00,450.00
EOF

curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@valid-records.csv" \
  -H "Accept: application/json"
```

Expected: 200 OK with `"valid": true` and no failures

## 6. CSV with Duplicate References

```bash
# Create a CSV with duplicate references
cat > duplicate-refs.csv << 'EOF'
Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
100001,NL91RABO0315273637,Payment,100.00,50.00,150.00
100001,NL27SNSB0917829871,Salary,200.00,100.00,300.00
100002,NL69ABNA0433647324,Groceries,500.00,-50.00,450.00
EOF

curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@duplicate-refs.csv" \
  -H "Accept: application/json"
```

Expected: 200 OK with validation failures for duplicate references

## 7. Health Check (if Spring Actuator is enabled)

```bash
curl http://localhost:8080/actuator/health
```


## Future Enhancements

This section outlines potential improvements and considerations for production deployment.

### Security

#### Authentication & Authorization
- **API Key Authentication**: Add API key validation for client identification
- **OAuth 2.0 / JWT**: Implement token-based authentication for user sessions
- **Role-Based Access Control (RBAC)**: Define roles (e.g., admin, auditor, processor) with different permissions
- **Rate Limiting**: Prevent abuse by limiting requests per client/IP

### Observability & Monitoring

#### Metrics (Micrometer/Prometheus)
- **Business Metrics**:
  - Total records processed (counter)
  - Validation success/failure rate (gauge)
  - File format distribution (CSV vs JSON)
  - Average file size processed
  - Validation errors by type (duplicates vs balance errors)
  
- **Performance Metrics**:
  - File parse time (histogram)
  - Validation time per record (histogram)
  - End-to-end request duration (timer)

#### Logging Enhancements
- **Log Aggregation**: Centralized logging (ELK Stack, Splunk, CloudWatch)
- **Audit Logging**: Track who uploaded what files and when

### Performance Optimization

#### Batch Processing
- **Virtual Threads (Java 21)**: Use Project Loom for lightweight concurrency
  ```java
  // Process multiple files concurrently
  try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      files.forEach(file -> executor.submit(() -> processFile(file)));
  }
  ```
- **Parallel Streams**: Process large files with parallel validation
- **Async Processing**: Return 202 Accepted with job ID, poll for results

#### Database Integration
- **Persistence Layer**: Store validation results for audit trail - Transaction records, Validation reports

### API Enhancements

#### Versioning
- **URL Versioning**: `/api/v2/transaction-records/validate`
- **Header Versioning**: `Accept: application/vnd.bank.v2+json`
- **Backward Compatibility**: Support multiple API versions

#### Response Enhancements
- **Pagination**: For large validation reports
- **Filtering**: Filter failures by type (duplicates only, balance errors only)
- **Sorting**: Sort failures by reference, amount, etc.

### Testing Enhancements

#### Additional Test Types
- **Load Testing**: JMeter, Gatling, or k6 for performance testing
- **Security Testing**: OWASP ZAP, Burp Suite for vulnerability scanning
- **End-to-End Tests**: Selenium/Playwright for full workflow testing

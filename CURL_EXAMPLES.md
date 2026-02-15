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

## 3. Pretty Print JSON Response

```bash
curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@test-data/records.csv" \
  -H "Accept: application/json" | jq '.'
```

## 4. Save Response to File

```bash
curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@test-data/records.json" \
  -H "Accept: application/json" \
  -o validation-report.json
```

## 5. Verbose Output (Show Headers)

```bash
curl -v -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@test-data/records.csv" \
  -H "Accept: application/json"
```

## 6. Empty File (Error Case - 400 Bad Request)

```bash
# Create an empty file
touch empty.csv

curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@empty.csv" \
  -H "Accept: application/json"
```

Expected: 400 Bad Request with error message "File is empty"

## 7. Missing File Parameter (Error Case - 400 Bad Request)

```bash
curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -H "Accept: application/json"
```

Expected: 400 Bad Request

## 8. Unsupported File Format (Error Case - 415 Unsupported Media Type)

```bash
# Create a text file
echo "This is not a valid format" > invalid.txt

curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@invalid.txt" \
  -H "Accept: application/json"
```

Expected: 415 Unsupported Media Type

## 9. Malformed CSV (Error Case - 400 Bad Request)

```bash
# Create a malformed CSV
cat > malformed.csv << 'EOF'
Reference,AccountNumber
123456,NL91RABO0315273637
EOF

curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@malformed.csv" \
  -H "Accept: application/json"
```

Expected: 400 Bad Request with parse error details

## 10. Malformed JSON (Error Case - 400 Bad Request)

```bash
# Create a malformed JSON
cat > malformed.json << 'EOF'
{invalid json}
EOF

curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@malformed.json" \
  -H "Accept: application/json"
```

Expected: 400 Bad Request with JSON parse error

## 11. Valid CSV with All Correct Records

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

## 12. CSV with Duplicate References

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

## 13. CSV with Incorrect End Balance

```bash
# Create a CSV with incorrect end balance
cat > incorrect-balance.csv << 'EOF'
Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
100001,NL91RABO0315273637,Payment,100.00,50.00,200.00
EOF

curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@incorrect-balance.csv" \
  -H "Accept: application/json"
```

Expected: 200 OK with validation failure for incorrect end balance

## 14. Check API Documentation (Swagger UI)

Open in browser:
```
http://localhost:8080/swagger-ui.html
```

Or get OpenAPI spec:
```bash
curl http://localhost:8080/v3/api-docs
```

## 15. Health Check (if Spring Actuator is enabled)

```bash
curl http://localhost:8080/actuator/health
```

## Response Format Example

Successful validation response:
```json
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "totalRecords": 10,
  "validRecords": 7,
  "failedRecords": 3,
  "valid": false,
  "failures": [
    {
      "reference": 112806,
      "accountNumber": "NL27SNSB0917829871",
      "description": "Clothes Irma Steven",
      "reasons": ["DUPLICATE_REFERENCE"]
    },
    {
      "reference": 167875,
      "accountNumber": "NL93ABNA0585619023",
      "description": "Toy Greg Alysha",
      "reasons": ["INCORRECT_END_BALANCE"]
    }
  ]
}
```

Error response (RFC 7807 Problem Details):
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "File is empty",
  "instance": "/api/v1/transaction-records/validate",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-02-12T16:30:00Z"
}
```

## Tips

1. **Use jq for pretty printing**: Install jq (`brew install jq` on macOS) to format JSON responses
2. **Check HTTP status codes**: Use `-w "\nHTTP Status: %{http_code}\n"` to see status codes
3. **Follow redirects**: Add `-L` flag if needed
4. **Timeout**: Add `--max-time 30` to set a 30-second timeout
5. **Silent mode**: Use `-s` to suppress progress meter

## Advanced Examples

### With timeout and status code:
```bash
curl -s -w "\nHTTP Status: %{http_code}\n" \
  --max-time 30 \
  -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@test-data/records.csv" \
  -H "Accept: application/json" | jq '.'
```

### Test from a different directory:
```bash
curl -X POST http://localhost:8080/api/v1/transaction-records/validate \
  -F "file=@/full/path/to/test-data/records.csv" \
  -H "Accept: application/json"
```

### Using a variable for the base URL:
```bash
BASE_URL="http://localhost:8080"
curl -X POST ${BASE_URL}/api/v1/transaction-records/validate \
  -F "file=@test-data/records.csv" \
  -H "Accept: application/json"
```

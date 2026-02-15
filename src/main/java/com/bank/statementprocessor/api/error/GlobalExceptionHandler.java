package com.bank.statementprocessor.api.error;

import com.bank.statementprocessor.api.dto.ProblemDetail;
import com.bank.statementprocessor.api.error.EmptyFileException;
import com.bank.statementprocessor.api.error.ParseException;
import com.bank.statementprocessor.api.error.UnsupportedFileFormatException;
import com.bank.statementprocessor.api.error.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;

/**
 * Global exception handler for the application.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handles ParseException - file parsing failures (malformed CSV/JSON).
     * Returns HTTP 400 with structured problem details.
     */
    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ProblemDetail> handleParseException(ParseException ex, WebRequest request) {
        // Client error - malformed file
        log.warn("Parse exception: {}", ex.getMessage());
        
        ProblemDetail problem = new ProblemDetail(
                HttpStatus.BAD_REQUEST.value(),
                "about:blank",
                "Parse Error",
                buildParseErrorMessage(ex),
                request.getDescription(false).replace("uri=", ""),
                Instant.now(),
                MDC.get("correlationId"),
                "PARSE_ERROR"
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
    
    /**
     * Handles EmptyFileException - empty file upload.
     * Returns HTTP 400 with structured problem details.
     */
    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ProblemDetail> handleEmptyFile(EmptyFileException ex, WebRequest request) {
        // Client error - empty file
        log.warn("Empty file: {}", ex.getMessage());
        
        ProblemDetail problem = new ProblemDetail(
                HttpStatus.BAD_REQUEST.value(),
                "about:blank",
                "Empty File",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                Instant.now(),
                MDC.get("correlationId"),
                "EMPTY_FILE"
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
    
    /**
     * Handles MissingServletRequestPartException - missing file parameter.
     * Returns HTTP 400 with structured problem details.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ProblemDetail> handleMissingFile(MissingServletRequestPartException ex, WebRequest request) {
        // Client error - missing parameter
        log.warn("Missing file parameter");
        
        ProblemDetail problem = new ProblemDetail(
                HttpStatus.BAD_REQUEST.value(),
                "about:blank",
                "Missing File",
                "File parameter is required",
                request.getDescription(false).replace("uri=", ""),
                Instant.now(),
                MDC.get("correlationId"),
                "MISSING_FILE"
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
    
    /**
     * Handles MaxUploadSizeExceededException - file too large.
     * Returns HTTP 413 with structured problem details.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleFileTooLarge(MaxUploadSizeExceededException ex, WebRequest request) {
        // Client error - file too large
        log.warn("File too large");
        
        ProblemDetail problem = new ProblemDetail(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "about:blank",
                "File Too Large",
                "File too large. Maximum size is 10MB",
                request.getDescription(false).replace("uri=", ""),
                Instant.now(),
                MDC.get("correlationId"),
                "FILE_TOO_LARGE"
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }
    
    /**
     * Handles UnsupportedFileFormatException - unsupported file format.
     * Returns HTTP 415 with structured problem details.
     */
    @ExceptionHandler(UnsupportedFileFormatException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedFileFormat(UnsupportedFileFormatException ex, WebRequest request) {
        // Client error - unsupported format
        log.warn("Unsupported file format: {}", ex.getMessage());
        
        ProblemDetail problem = new ProblemDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "about:blank",
                "Unsupported File Format",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                Instant.now(),
                MDC.get("correlationId"),
                "UNSUPPORTED_FORMAT"
        );
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problem);
    }
    
    /**
     * Handles ValidationException - unexpected validation errors.
     * Returns HTTP 500 with structured problem details.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(ValidationException ex, WebRequest request) {
        // Server error - unexpected failure
        log.error("Validation exception", ex);
        
        ProblemDetail problem = new ProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "about:blank",
                "Validation Error",
                "An unexpected error occurred during validation",
                request.getDescription(false).replace("uri=", ""),
                Instant.now(),
                MDC.get("correlationId"),
                "VALIDATION_ERROR"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
    
    /**
     * Handles all other exceptions - generic error handler.
     * Returns HTTP 500 with structured problem details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, WebRequest request) {
        // Server error - unexpected exception
        log.error("Unexpected exception occurred", ex);
        
        ProblemDetail problem = new ProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "about:blank",
                "Internal Server Error",
                "An unexpected error occurred",
                request.getDescription(false).replace("uri=", ""),
                Instant.now(),
                MDC.get("correlationId"),
                "INTERNAL_ERROR"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
    
    /**
     * Builds a detailed error message for ParseException.
     */
    private String buildParseErrorMessage(ParseException ex) {
        StringBuilder message = new StringBuilder("Failed to parse file: ");
        message.append(ex.getMessage());
        
        if (ex.getLineNumber() > 0) {
            message.append(" at line ").append(ex.getLineNumber());
        }
        
        if (ex.getFieldName() != null) {
            message.append(", field: ").append(ex.getFieldName());
        }
        
        return message.toString();
    }
}

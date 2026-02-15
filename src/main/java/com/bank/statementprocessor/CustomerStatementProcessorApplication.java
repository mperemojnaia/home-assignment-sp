package com.bank.statementprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the Customer Statement Processor.
 * This service validates bank transaction records from uploaded CSV or JSON files.
 */
@SpringBootApplication
public class CustomerStatementProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerStatementProcessorApplication.class, args);
    }
}

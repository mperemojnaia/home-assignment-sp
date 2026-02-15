package com.bank.statementprocessor.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAPI 3.0 documentation.
 * <p>
 * Configures the API metadata including title, description, version, and contact information
 * as specified in the design document.
 */
@Configuration
public class OpenApiConfig {
    
    /**
     * Configures the OpenAPI documentation metadata.
     * 
     * @return OpenAPI configuration with metadata
     */
    @Bean
    public OpenAPI customerStatementProcessorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Customer Statement Processor API")
                        .description("REST API for validating bank transaction records from CSV or JSON files")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")
                ));
    }
}

package com.bank.statementprocessor.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pact provider verification tests for Transaction Record Validation API.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("transaction-record-validator")
@PactFolder("src/test/resources/pacts")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransactionRecordValidationPactProviderTest {
    
    @LocalServerPort
    private int port;
    
    @BeforeAll
    void verifyPactsPresent() throws IOException {
        Path pactsDir = Path.of("src/test/resources/pacts");
        assertThat(Files.exists(pactsDir))
                .as("Pacts directory must exist at %s", pactsDir)
                .isTrue();
        
        try (var stream = Files.list(pactsDir)) {
            assertThat(stream.anyMatch(p -> p.toString().endsWith(".json")))
                    .as("At least one pact file (*.json) must exist in %s", pactsDir)
                    .isTrue();
        }
    }
    
    @BeforeEach
    void setup(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }
    
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPactInteraction(PactVerificationContext context) {
        // Verify each interaction defined in the pact files
        context.verifyInteraction();
    }
}

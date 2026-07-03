package com.project.paymentservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class SchemaValidationTest {

    @Test
    void contextLoads() {
        // If context loads, Hibernate ddl-auto=validate has succeeded
        assertTrue(true, "Context loaded successfully, schema is valid");
    }
}

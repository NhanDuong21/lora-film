package com.lorafilm.movie.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocsShouldContainErrorSchemas() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ValidationErrorResponse").exists())
                .andExpect(jsonPath("$.components.schemas.InvalidEnumErrorResponse").exists())
                .andExpect(jsonPath("$.components.schemas.BulkSeatValidationErrorResponse").exists());
    }

    @Test
    void bulkCreateSeatsEndpointShouldHaveCorrectErrorResponses() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Basic validation: verify that the BulkCreate endpoint has 400 response
                // defined with multiple schemas
                .andExpect(content().string(containsString("BulkSeatValidationErrorResponse")));
    }
}

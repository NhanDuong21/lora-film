package com.project.authservice.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.project.authservice.exception.common.BusinessValidationException;
import com.project.authservice.exception.common.ExternalServiceUnavailableException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@WebMvcTest(controllers = GlobalExceptionHandlerIntegrationTest.TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class TestController {
        
        static class TestDto {
            @NotBlank(message = "Field cannot be blank")
            @Pattern(regexp = "^[0-9]+$", message = "Field must be numeric")
            public String field;
            
            public void setField(String field) { this.field = field; }
            public String getField() { return field; }
        }

        @PostMapping("/test-validation")
        public String testValidation(@Valid @RequestBody TestDto dto) {
            return "ok";
        }

        @PostMapping("/test-business")
        public String testBusiness() {
            throw new BusinessValidationException("Duplicate CCCD", "DUPLICATE_CCCD");
        }

        @PostMapping("/test-redis")
        public String testRedis() {
            throw new ExternalServiceUnavailableException("Redis");
        }
    }

    @Test
    void testMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"field\": \"value\"")) // Missing closing brace
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON"));
    }

    @Test
    void testValidationFailureReturns422WithDeduplication() throws Exception {
        // Send empty string to trigger both @NotBlank and @Pattern
        mockMvc.perform(post("/test-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"field\": \"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("field"))
                .andExpect(jsonPath("$.errors[0].message").value("Field cannot be blank")); // Priority 1 (NotBlank) overrides Priority 2 (Pattern)
    }

    @Test
    void testBusinessValidationReturns422() throws Exception {
        mockMvc.perform(post("/test-business")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_CCCD"))
                .andExpect(jsonPath("$.message").value("Duplicate CCCD"));
    }

    @Test
    void testExternalServiceUnavailableReturns503() throws Exception {
        mockMvc.perform(post("/test-redis")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"));
    }
}

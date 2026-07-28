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

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(
    controllers = GlobalExceptionHandlerIntegrationTest.TestController.class,
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = com.project.authservice.config.SecurityConfig.class
    ),
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
    }
)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerIntegrationTest.TestController.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.project.authservice.util.JwtUtil jwtUtil;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository;

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

        static class TestMultipleDto {
            @NotBlank(message = "Field1 cannot be blank")
            public String field1;
            
            @NotBlank(message = "Field2 cannot be blank")
            public String field2;
            
            public void setField1(String field1) { this.field1 = field1; }
            public String getField1() { return field1; }
            public void setField2(String field2) { this.field2 = field2; }
            public String getField2() { return field2; }
        }

        @PostMapping("/test-multiple-fields")
        public String testMultipleValidation(@Valid @RequestBody TestMultipleDto dto) {
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

        @PostMapping("/test-kafka")
        public String testKafka() {
            throw new org.springframework.kafka.KafkaException("Kafka timeout");
        }
    }

    @Test
    void testMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"field\": \"value\"")) // Missing closing brace
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stacktrace").doesNotExist());
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
                .andExpect(jsonPath("$.errors[0].code").value("NotBlank")) // Validate code is present
                .andExpect(jsonPath("$.errors[0].message").value("Field cannot be blank")); // Priority 1 (NotBlank) overrides Priority 2 (Pattern)
    }

    @Test
    void testBusinessValidationReturns422() throws Exception {
        mockMvc.perform(post("/test-business")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_CCCD"))
                .andExpect(jsonPath("$.message").value("Duplicate CCCD"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testExternalServiceUnavailableReturns503() throws Exception {
        mockMvc.perform(post("/test-redis")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testMultipleInvalidFieldsPreserved() throws Exception {
        mockMvc.perform(post("/test-multiple-fields")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"field1\": \"\", \"field2\": \"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'field1')].code").value("NotBlank"))
                .andExpect(jsonPath("$.errors[?(@.field == 'field2')].code").value("NotBlank"));
    }

    @Test
    void testGatewayTimeoutReturns504() throws Exception {
        mockMvc.perform(post("/test-kafka")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.errorCode").value("GATEWAY_TIMEOUT"))
                .andExpect(jsonPath("$.success").value(false));
    }
}

package com.project.authservice.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.exception.common.ExternalServiceUnavailableException;
import com.project.authservice.exception.common.GatewayTimeoutException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleExternalServiceUnavailableException() {
        ExternalServiceUnavailableException ex = new ExternalServiceUnavailableException("Redis");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleExternalServiceUnavailableException(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Service is currently unavailable. Please try again later.", response.getBody().getMessage());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().getErrorCode());
    }

    @Test
    void testHandleGatewayTimeoutException() {
        GatewayTimeoutException ex = new GatewayTimeoutException("Kafka");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleGatewayTimeoutException(ex);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals("Gateway timeout. Please try again later.", response.getBody().getMessage());
        assertEquals("GATEWAY_TIMEOUT", response.getBody().getErrorCode());
    }

    @Test
    void testValidationPriorityAlgorithm_MethodArgumentNotValidException() throws NoSuchMethodException {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        MethodParameter methodParameter = new MethodParameter(this.getClass().getDeclaredMethod("dummyMethod"), -1);
        
        // Priority 1: NotNull, NotBlank
        // Priority 2: Pattern, Email
        FieldError error1 = new FieldError("user", "phoneNumber", "phoneNumber invalid format");
        error1 = new FieldError("user", "phoneNumber", null, false, new String[]{"Pattern"}, null, "phoneNumber invalid format");

        FieldError error2 = new FieldError("user", "phoneNumber", "phoneNumber is required");
        error2 = new FieldError("user", "phoneNumber", null, false, new String[]{"NotBlank"}, null, "phoneNumber is required");

        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));
        
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleMethodArgumentNotValidException(ex);

        // Assert
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().getMessage());
        List<ApiResponse.ValidationError> errors = response.getBody().getErrors();
        assertEquals(1, errors.size());
        assertEquals("phoneNumber", errors.get(0).getField());
        // NotBlank (Priority 1) should override Pattern (Priority 2)
        assertEquals("phoneNumber is required", errors.get(0).getMessage());
    }

    private void dummyMethod() {
    }
}

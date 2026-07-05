package com.project.paymentservice.exception;

import com.project.paymentservice.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        logger.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = ex.getHttpStatus();
        if (status == null) {
            status = resolveStatusFromCode(ex.getErrorCode());
        }

        ApiResponse<Object> response = new ApiResponse<>(
                false, ex.getMessage(), ex.getErrorCode(), ex.getData(), null);
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            Map<String, String> fieldError = new HashMap<>();
            fieldError.put("field", error.getField());
            fieldError.put("message", error.getDefaultMessage());
            errors.add(fieldError);
        });
        return new ResponseEntity<>(
                ApiResponse.error("Validation failed", "VALIDATION_ERROR", errors),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        logger.warn("Missing header: {}", ex.getHeaderName());
        String errorCode = "Idempotency-Key".equalsIgnoreCase(ex.getHeaderName())
                ? "IDEMPOTENCY_KEY_REQUIRED" : "MISSING_HEADER";
        return new ResponseEntity<>(
                ApiResponse.error("Missing required header: " + ex.getHeaderName(), errorCode),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        logger.warn("Optimistic locking failure: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("Concurrent update conflict, please retry", "CONCURRENT_CONFLICT"),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = String.format("Invalid value for parameter '%s'", ex.getName());
        return new ResponseEntity<>(
                ApiResponse.error(msg, "VALIDATION_ERROR"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(
                ApiResponse.error("Malformed JSON request", "VALIDATION_ERROR"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex) {
        return new ResponseEntity<>(
                ApiResponse.error("Resource not found", "NOT_FOUND"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Throwable ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                ApiResponse.error("An unexpected error occurred", "INTERNAL_SERVER_ERROR"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus resolveStatusFromCode(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (errorCode) {
            case "UNAUTHORIZED", "INTERNAL_TOKEN_INVALID" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "PAYMENT_NOT_FOUND", "BOOKING_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "PAYMENT_ACTIVE_ATTEMPT_EXISTS", "PAYMENT_RETRY_TEMPORARILY_BLOCKED",
                 "PAYMENT_CANNOT_BE_CANCELLED", "IDEMPOTENCY_KEY_REUSED",
                 "IDEMPOTENCY_REQUEST_IN_PROGRESS", "BOOKING_NOT_PAYABLE" -> HttpStatus.CONFLICT;
            case "PAYMENT_SESSION_CREATION_FAILED" -> HttpStatus.BAD_GATEWAY;
            case "BOOKING_SERVICE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}

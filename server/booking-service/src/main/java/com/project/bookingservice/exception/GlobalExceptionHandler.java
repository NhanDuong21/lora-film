package com.project.bookingservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.bookingservice.common.ApiResponse;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.MissingRequestHeaderException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        logger.warn("Business exception occurred: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorCode = ex.getErrorCode();

        if ("BOOKING_IDEMPOTENCY_CONFLICT".equals(errorCode) ||
            "BOOKING_SHOWTIME_NOT_AVAILABLE".equals(errorCode) ||
            "BOOKING_SEAT_ALREADY_BOOKED".equals(errorCode) ||
            "BOOKING_SEAT_ALREADY_HELD".equals(errorCode) ||
            "SEAT_RESERVATION_ALREADY_CONVERTED".equals(errorCode) ||
            "SEAT_RESERVATION_EXPIRED".equals(errorCode)) {
            status = HttpStatus.CONFLICT;
        } else if ("FORBIDDEN".equals(errorCode) || "UNAUTHORIZED".equals(errorCode)) {
            status = "UNAUTHORIZED".equals(errorCode) ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        } else if (errorCode != null && errorCode.contains("NOT_FOUND")) {
            status = HttpStatus.NOT_FOUND;
        }

        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage() != null ? ex.getMessage() : "Business logic error", errorCode, ex.getData(), null);
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("Invalid request parameters", "VALIDATION_ERROR"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException ex) {
        logger.warn("Optimistic locking failure: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("Concurrent update conflict", "BOOKING_OPTIMISTIC_LOCK_CONFLICT"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({
        RedisConnectionFailureException.class, 
        org.springframework.data.redis.RedisSystemException.class,
        io.lettuce.core.RedisCommandTimeoutException.class,
        org.springframework.dao.QueryTimeoutException.class,
        io.lettuce.core.RedisConnectionException.class,
        java.util.concurrent.TimeoutException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleRedisException(Exception ex) {
        logger.error("Redis connection error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiResponse.error("Seat lock service is temporarily unavailable.", "SEAT_LOCK_SERVICE_UNAVAILABLE"), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        logger.warn("Missing request header: {}", ex.getHeaderName());
        String errorCode = "Idempotency-Key".equalsIgnoreCase(ex.getHeaderName()) ? "BOOKING_IDEMPOTENCY_KEY_REQUIRED" : "MISSING_HEADER";
        return new ResponseEntity<>(ApiResponse.error("Missing required header: " + ex.getHeaderName(), errorCode), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        logger.warn("Type mismatch error: {} = {}", ex.getName(), ex.getValue());
        return new ResponseEntity<>(ApiResponse.error("Invalid path variable or parameter format: " + ex.getName(), "INVALID_PARAM_FORMAT"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof org.springframework.data.redis.RedisConnectionFailureException ||
                cause instanceof io.lettuce.core.RedisConnectionException ||
                cause instanceof java.net.ConnectException ||
                (cause.getClass().getName().contains("RedisSystemException"))) {
                return new ResponseEntity<>(ApiResponse.error("Seat lock service is temporarily unavailable.", "SEAT_LOCK_SERVICE_UNAVAILABLE"), HttpStatus.SERVICE_UNAVAILABLE);
            }
            cause = cause.getCause();
        }

        return new ResponseEntity<>(ApiResponse.error("An unexpected error occurred", "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

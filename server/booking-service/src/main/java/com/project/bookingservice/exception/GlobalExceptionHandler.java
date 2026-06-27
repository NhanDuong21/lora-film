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
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        logger.warn("Business exception occurred: {}", ex.getErrorCode(), ex);
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorCode = ex.getErrorCode();

        if ("BOOKING_IDEMPOTENCY_CONFLICT".equals(errorCode) ||
            "BOOKING_SHOWTIME_NOT_AVAILABLE".equals(errorCode) ||
            "BOOKING_SEAT_ALREADY_BOOKED".equals(errorCode) ||
            "BOOKING_SEAT_ALREADY_HELD".equals(errorCode) ||
            "SEAT_RESERVATION_ALREADY_CONVERTED".equals(errorCode)) {
            status = HttpStatus.CONFLICT;
        } else if ("FORBIDDEN".equals(errorCode) || "UNAUTHORIZED".equals(errorCode)) {
            status = "UNAUTHORIZED".equals(errorCode) ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        } else if (errorCode != null && errorCode.contains("NOT_FOUND")) {
            status = HttpStatus.NOT_FOUND;
        }

        return new ResponseEntity<>(ApiResponse.error(ex.getMessage() != null ? ex.getMessage() : "Business logic error", errorCode), status);
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

    @ExceptionHandler({RedisConnectionFailureException.class, org.springframework.data.redis.RedisSystemException.class})
    public ResponseEntity<ApiResponse<Void>> handleRedisException(Exception ex) {
        logger.error("Redis connection error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiResponse.error("Service unavailable", "SEAT_LOCK_SERVICE_UNAVAILABLE"), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        logger.warn("Missing request header: {}", ex.getHeaderName());
        String errorCode = "Idempotency-Key".equalsIgnoreCase(ex.getHeaderName()) ? "BOOKING_IDEMPOTENCY_KEY_REQUIRED" : "MISSING_HEADER";
        return new ResponseEntity<>(ApiResponse.error("Missing required header: " + ex.getHeaderName(), errorCode), HttpStatus.BAD_REQUEST);
    }
}

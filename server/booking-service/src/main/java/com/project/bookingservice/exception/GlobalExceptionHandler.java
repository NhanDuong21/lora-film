package com.project.bookingservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException ex) {
        logger.warn("Business exception occurred: {}", ex.getErrorCode(), ex);
        Map<String, String> body = new HashMap<>();
        body.put("errorCode", ex.getErrorCode());
        if (ex.getMessage() != null && !ex.getMessage().equals(ex.getErrorCode())) {
            body.put("message", ex.getMessage());
        }

        HttpStatus status = HttpStatus.BAD_REQUEST;
        if ("BOOKING_IDEMPOTENCY_CONFLICT".equals(ex.getErrorCode())) {
            status = HttpStatus.CONFLICT;
        } else if ("FORBIDDEN".equals(ex.getErrorCode())) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex.getErrorCode().contains("NOT_FOUND")) {
            status = HttpStatus.NOT_FOUND;
        }

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("errorCode", "VALIDATION_ERROR");
        body.put("message", "Invalid request parameters");
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}

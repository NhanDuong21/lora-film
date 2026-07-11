package com.lorafilm.movie.common.exception;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.api.FieldErrorDetail;
import com.lorafilm.movie.common.api.InvalidDateFormatData;
import com.lorafilm.movie.common.api.InvalidEnumErrorData;
import com.lorafilm.movie.common.api.ValidationErrorData;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Comparator;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.error("BusinessException [{}]: {}", errorCode.name(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.valueOf(errorCode.getHttpStatus()))
                .body(ApiResponse.fail(ex.getMessage(), errorCode.name(), ex.getErrorData()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorData>> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getRejectedValue(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldErrorDetail::field))
                .collect(Collectors.toList());

        log.error("ValidationException: {}", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("Request validation failed", ErrorCode.VALIDATION_ERROR.name(), new ValidationErrorData(fieldErrors)));
    }

    private String buildJsonPath(List<com.fasterxml.jackson.databind.JsonMappingException.Reference> path) {
        StringBuilder result = new StringBuilder();
        for (com.fasterxml.jackson.databind.JsonMappingException.Reference reference : path) {
            if (reference.getFieldName() != null) {
                if (!result.isEmpty()) {
                    result.append(".");
                }
                result.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                result.append("[").append(reference.getIndex()).append("]");
            }
        }
        return result.toString();
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException: {}", ex.getMessage());
        
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String fieldName = buildJsonPath(ife.getPath());
                List<String> allowedValues = Arrays.stream(ife.getTargetType().getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.toList());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Invalid enum value", "INVALID_ENUM_VALUE", 
                            new InvalidEnumErrorData(fieldName, ife.getValue(), allowedValues)));
            } else if (ife.getTargetType() != null && (ife.getTargetType().getName().contains("Instant") || ife.getTargetType().getName().contains("Date") || ife.getTargetType().getName().contains("Time"))) {
                String fieldName = buildJsonPath(ife.getPath());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Invalid date-time format", "INVALID_DATE_TIME_FORMAT", 
                            new InvalidDateFormatData(fieldName, ife.getValue(), "yyyy-MM-dd'T'HH:mm:ss (or valid ISO format)")));
            }
        }
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("Malformed JSON request", "MALFORMED_JSON"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("DataIntegrityViolationException: ", ex);
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        
        if (rootMsg != null) {
            if (rootMsg.contains("uk_auditoriums_cinema_name")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ErrorCode.AUDITORIUM_NAME_DUPLICATED.getMessage(), ErrorCode.AUDITORIUM_NAME_DUPLICATED.name()));
            }
            if (rootMsg.contains("uk_seats_auditorium_code")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ErrorCode.DUPLICATE_SEAT_CODE.getMessage(), ErrorCode.DUPLICATE_SEAT_CODE.name()));
            }
            if (rootMsg.contains("uk_seats_auditorium_position")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ErrorCode.DUPLICATE_SEAT_POSITION.getMessage(), ErrorCode.DUPLICATE_SEAT_POSITION.name()));
            }
        }
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("Data integrity constraint violated", "DATA_INTEGRITY_VIOLATION"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled Exception: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR.name()));
    }
}
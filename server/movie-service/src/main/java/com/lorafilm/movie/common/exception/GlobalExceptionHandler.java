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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
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
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.error("BusinessException [{}]: {}", errorCode.name(), ex.getMessage());

        if (ex.getErrorData() != null) {
            return ResponseEntity
                    .status(HttpStatus.valueOf(errorCode.getHttpStatus()))
                    .body(ApiResponse.fail(errorCode.name(), ex.getMessage(), ex.getErrorData()));
        }

        return ResponseEntity
                .status(HttpStatus.valueOf(errorCode.getHttpStatus()))
                .body(ApiResponse.fail(errorCode.name(), ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("ResourceNotFoundException: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND.name(), ex.getMessage()));
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
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.name(), "Request validation failed", new ValidationErrorData(fieldErrors)));
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException: {}", ex.getMessage());
        
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String fieldName = buildJsonPath(ife.getPath());
                List<String> allowedValues = Arrays.stream(ife.getTargetType().getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.toList());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("INVALID_ENUM_VALUE", "Invalid enum value", 
                            new InvalidEnumErrorData(fieldName, ife.getValue(), allowedValues)));
            } else if (ife.getTargetType() != null && (ife.getTargetType().getName().contains("Instant") || ife.getTargetType().getName().contains("Date") || ife.getTargetType().getName().contains("Time"))) {
                String fieldName = buildJsonPath(ife.getPath());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("INVALID_DATE_TIME_FORMAT", "Invalid date-time format", 
                            new InvalidDateFormatData(fieldName, ife.getValue(), "yyyy-MM-dd'T'HH:mm:ss (or valid ISO format)")));
            }
        }
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("MALFORMED_JSON", "Malformed JSON request"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        log.error("HttpMediaTypeNotSupportedException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.name(), "Unsupported media type"));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(RuntimeException ex) {
        log.error("IllegalArgument/StateException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.name(), ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        String name = ex.getParameterName();
        log.error("MissingServletRequestParameterException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.name(), "Missing required parameter: " + name));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.error("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.name(), "Invalid parameter type for '" + ex.getName() + "'"));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        log.error("ConstraintViolationException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.name(), "Validation error: " + ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.data.mapping.PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<Void>> handlePropertyReferenceException(org.springframework.data.mapping.PropertyReferenceException ex) {
        log.error("PropertyReferenceException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.name(), "Invalid sort property: " + ex.getPropertyName()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("DataIntegrityViolationException: ", ex);
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        
        if (rootMsg != null) {
            if (rootMsg.contains("uk_auditoriums_cinema_name")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ErrorCode.AUDITORIUM_NAME_DUPLICATED.name(), ErrorCode.AUDITORIUM_NAME_DUPLICATED.getMessage()));
            }
            if (rootMsg.contains("uk_seats_auditorium_code")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ErrorCode.DUPLICATE_SEAT_CODE.name(), ErrorCode.DUPLICATE_SEAT_CODE.getMessage()));
            }
            if (rootMsg.contains("uk_seats_auditorium_position")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ErrorCode.DUPLICATE_SEAT_POSITION.name(), ErrorCode.DUPLICATE_SEAT_POSITION.getMessage()));
            }
            if (rootMsg.contains("uk_movies_active_slug")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail("MOVIE_TITLE_DUPLICATED", "Tên phim này đã tồn tại trong hệ thống (trùng tiêu đề)"));
            }
            if (rootMsg.contains("uk_movie_version_unique")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail("MOVIE_VERSION_DUPLICATED", "Phiên bản phim với định dạng và ngôn ngữ này đã tồn tại"));
            }
            if (rootMsg.contains("uk_movie_credit_unique")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail("MOVIE_CREDIT_DUPLICATED", "Nhân sự này đã được gán vai trò này trong phim"));
            }
            if (rootMsg.contains("uk_production_companies_name")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail("COMPANY_DUPLICATED", "Tên công ty sản xuất này đã tồn tại"));
            }
            if (rootMsg.contains("movie_production_companies") && (rootMsg.contains("PRIMARY") || rootMsg.contains("Duplicate entry"))) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail("MOVIE_COMPANY_DUPLICATED", "Công ty sản xuất này đã được gán cho phim với vai trò này"));
            }
        }
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("DATA_INTEGRITY_VIOLATION", "Data integrity constraint violated"));
    }

    @ExceptionHandler({
            org.hibernate.exception.LockAcquisitionException.class,
            jakarta.persistence.PessimisticLockException.class,
            jakarta.persistence.LockTimeoutException.class,
            org.springframework.dao.CannotAcquireLockException.class,
            org.springframework.dao.PessimisticLockingFailureException.class,
            org.springframework.dao.DeadlockLoserDataAccessException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleLockingExceptions(Exception ex) {
        log.error("Locking Exception: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ErrorCode.SHOWTIME_SCHEDULING_CONFLICT.name(), ErrorCode.SHOWTIME_SCHEDULING_CONFLICT.getMessage()));
    }

    @ExceptionHandler({
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingException(Exception ex) {
        log.error("Auto schedule preview optimistic locking conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(
                        ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT.name(),
                        ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT.getMessage()
                ));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(org.springframework.dao.DataAccessException ex) {
        log.error("DataAccessException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.name(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled Exception: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.name(), ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
    }
}

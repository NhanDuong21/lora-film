package com.project.notificationservice.exception;

import com.project.notificationservice.common.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business Exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), ex.getErrorCode());
        response.setData(ex.getData());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiResponse<Object>> handleOptimisticLockException(Exception ex) {
        log.warn("Optimistic Lock Conflict: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("Optimistic lock conflict occurred. The entity has been updated by another process.", "NOTIFICATION_OPTIMISTIC_LOCK_CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.validation.BindException.class})
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(Exception ex) {
        org.springframework.validation.BindingResult bindingResult;
        if (ex instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException) ex).getBindingResult();
        } else {
            bindingResult = ((org.springframework.validation.BindException) ex).getBindingResult();
        }

        boolean hasInvalidChannelError = bindingResult.getFieldErrors().stream()
                .anyMatch(fieldError -> {
                    if (!"channelType".equals(fieldError.getField())) {
                        return false;
                    }
                    if ("typeMismatch".equals(fieldError.getCode())) {
                        return true;
                    }
                    String defaultMsg = fieldError.getDefaultMessage();
                    return defaultMsg != null && (
                            defaultMsg.contains("NotificationChannel") ||
                            defaultMsg.contains("Failed to convert") ||
                            defaultMsg.contains("typeMismatch")
                    );
                });

        if (hasInvalidChannelError) {
            log.warn("Invalid notification channel type provided in validation");
            ApiResponse<Object> response = ApiResponse.error("Invalid notification channel type provided", "NOTIFICATION_INVALID_CHANNEL");
            return ResponseEntity.badRequest().body(response);
        }

        List<ApiResponse.ValidationError> validationErrors = bindingResult.getFieldErrors().stream()
                .map(fieldError -> new ApiResponse.ValidationError(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("Validation failed: {} field errors", validationErrors.size());
        ApiResponse<Object> response = ApiResponse.validationError("Validation failed", validationErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        List<ApiResponse.ValidationError> validationErrors = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    String field = path.substring(path.lastIndexOf('.') + 1);
                    return new ApiResponse.ValidationError(field, violation.getMessage());
                })
                .collect(Collectors.toList());
        ApiResponse<Object> response = ApiResponse.validationError("Validation failed", validationErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Message not readable: {}", ex.getMessage());
        String msg = ex.getMessage();
        if (msg != null && msg.contains("NotificationChannel")) {
            ApiResponse<Object> response = ApiResponse.error("Invalid notification channel type provided", "NOTIFICATION_INVALID_CHANNEL");
            return ResponseEntity.badRequest().body(response);
        }
        ApiResponse<Object> response = ApiResponse.error("Invalid JSON format or unreadable request body", "VALIDATION_ERROR");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("Forbidden", "FORBIDDEN");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), "METHOD_NOT_SUPPORTED");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Database integrity violation: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("Notification template code already exists", "NOTIFICATION_TEMPLATE_CODE_ALREADY_EXISTS");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("Resource not found", "NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ApiResponse<Object> response = ApiResponse.error("Internal server error", "INTERNAL_SERVER_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

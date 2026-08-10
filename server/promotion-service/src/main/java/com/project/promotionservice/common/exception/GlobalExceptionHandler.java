package com.project.promotionservice.common.exception;

import com.project.promotionservice.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        log.warn("BaseException occurred: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException occurred: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        log.warn("ValidationException occurred: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationException(IntegrationException ex) {
        log.warn("IntegrationException occurred: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation failed for request: {}", ex.getMessage(), ex);
        List<ErrorResponse.ValidationErrorDetail> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                details.add(new ErrorResponse.ValidationErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
        );
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED, "Invalid request parameters", details);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage(), ex);
        List<ErrorResponse.ValidationErrorDetail> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation ->
                details.add(new ErrorResponse.ValidationErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
        );
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.CONSTRAINT_VIOLATION, "Constraint validation failed", details);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid request parameter {}: {}", ex.getName(), ex.getValue(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.INVALID_REQUEST_PARAMETER,
                "Invalid value for parameter: " + ex.getName());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.MALFORMED_REQUEST_BODY,
                "Request body is malformed or contains an unsupported value");
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockFailure(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.CONCURRENCY_CONFLICT, "The resource was updated by another transaction. Please try again.");
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("AccessDeniedException occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.FORBIDDEN, "Access denied: " + ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled RuntimeException occurred: ", ex);
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled Exception occurred: ", ex);
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

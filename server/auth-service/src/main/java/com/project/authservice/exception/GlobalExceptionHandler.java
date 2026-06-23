package com.project.authservice.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.authservice.common.ApiResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Handles invalid birthday format (HTTP 400).
	 *
	 * <p>
	 * Returns the exact contract shape:
	 * 
	 * <pre>
	 * { "success": false, "message": "Birthday must be in YYYY-MM-DD format", "data": null }
	 * </pre>
	 *
	 * @param exception the birthday format exception
	 * @return 400 Bad Request
	 */
	@ExceptionHandler(InvalidBirthdayFormatException.class)
	public ResponseEntity<ApiResponse<Object>> handleInvalidBirthdayFormatException(
			InvalidBirthdayFormatException exception) {
		log.warn("Invalid birthday format: {}", exception.getMessage());
		// ApiResponse.failure() produces { success:false, message:..., data:null }
		// with no errorCode field – matching the required contract exactly.
		ApiResponse<Object> response = ApiResponse.failure(exception.getMessage());
		return ResponseEntity.badRequest().body(response);
	}

	/**
	 * Handles specific unverified account errors.
	 *
	 * @param exception auth exception
	 * @return structured error response with accountId
	 */
	@ExceptionHandler(AccountNotVerifiedException.class)
	public ResponseEntity<ApiResponse<Object>> handleAccountNotVerifiedException(AccountNotVerifiedException exception) {
		log.warn("Auth Exception [{}]: {}", exception.getErrorCode(), exception.getMessage());
		java.util.Map<String, Long> data = new java.util.HashMap<>();
		data.put("accountId", exception.getAccountId());
		ApiResponse<Object> response = new ApiResponse<>(false, exception.getMessage(), exception.getErrorCode(), data, null);
		return ResponseEntity.status(exception.getStatus()).body(response);
	}

	/**
	 * Handles specific OTP rate limit errors.
	 *
	 * @param exception auth exception
	 * @return structured error response with retryAfter
	 */
	@ExceptionHandler(OtpRateLimitException.class)
	public ResponseEntity<ApiResponse<Object>> handleOtpRateLimitException(OtpRateLimitException exception) {
		log.warn("Auth Exception [{}]: {}", exception.getErrorCode(), exception.getMessage());
		java.util.Map<String, Long> data = new java.util.HashMap<>();
		data.put("retryAfter", exception.getRetryAfter());
		ApiResponse<Object> response = new ApiResponse<>(false, exception.getMessage(), exception.getErrorCode(), data, null);
		return ResponseEntity.status(exception.getStatus()).body(response);
	}

	/**
	 * Handles standard API contract exceptions (BaseAuthException).
	 *
	 * @param exception auth exception
	 * @return structured error response
	 */
	@ExceptionHandler(BaseAuthException.class)
	public ResponseEntity<ApiResponse<Object>> handleBaseAuthException(BaseAuthException exception) {
		log.warn("Auth Exception [{}]: {}", exception.getErrorCode(), exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), exception.getErrorCode());
		return ResponseEntity.status(exception.getStatus()).body(response);
	}

	/**
	 * Handles business rule violations (legacy fallback).
	 *
	 * @param exception business exception
	 * @return error response
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException exception) {
		log.warn("Business exception: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), "BUSINESS_ERROR");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	/**
	 * Handles unauthorized access (legacy fallback).
	 *
	 * @param exception unauthorized exception
	 * @return error response
	 */
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException exception) {
		log.warn("Unauthorized: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), "UNAUTHORIZED");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	/**
	 * Handles missing resources (legacy fallback).
	 *
	 * @param exception resource not found exception
	 * @return error response
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException exception) {
		log.warn("Resource not found: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), "RESOURCE_NOT_FOUND");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	/**
	 * Handles validation errors from request body binding.
	 *
	 * @param exception validation exception
	 * @return structured validation error response
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException exception) {
		List<ApiResponse.ValidationError> validationErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> new ApiResponse.ValidationError(fieldError.getField(),
						fieldError.getDefaultMessage()))
				.collect(Collectors.toList());

		log.warn("Validation failed: {} field errors", validationErrors.size());
		ApiResponse<Object> response = ApiResponse.validationError("Validation failed", validationErrors);
		return ResponseEntity.badRequest().body(response);
	}

	/**
	 * Handles validation errors from parameter constraints.
	 *
	 * @param exception constraint violation exception
	 * @return error response
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
			ConstraintViolationException exception) {
		log.warn("Constraint violation: {}", exception.getMessage());
		List<ApiResponse.ValidationError> validationErrors = exception.getConstraintViolations().stream()
				.map(violation -> {
					String path = violation.getPropertyPath().toString();
					String field = path.substring(path.lastIndexOf('.') + 1);
					return new ApiResponse.ValidationError(field, violation.getMessage());
				})
				.collect(Collectors.toList());
		ApiResponse<Object> response = ApiResponse.validationError("Validation failed", validationErrors);
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(
			org.springframework.web.HttpRequestMethodNotSupportedException exception) {
		log.warn("Method not supported: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), "METHOD_NOT_SUPPORTED");
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
	}

	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
			org.springframework.http.converter.HttpMessageNotReadableException exception) {
		log.warn("Message not readable: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Invalid JSON format or unreadable request body", "VALIDATION_ERROR");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * Handles specific registration conflict errors (reserved data or existing data).
	 */
	@ExceptionHandler(RegistrationConflictException.class)
	public ResponseEntity<ApiResponse<Object>> handleRegistrationConflictException(RegistrationConflictException exception) {
		log.warn("Registration Conflict [{}]: {}", exception.getErrorCode(), exception.getMessage());
		
		java.util.Map<String, Long> data = null;
		if (exception.getRetryAfterSeconds() != null) {
			data = new java.util.HashMap<>();
			data.put("retryAfterSeconds", exception.getRetryAfterSeconds());
		}
		
		ApiResponse<Object> response = new ApiResponse<>(false, exception.getMessage(), exception.getErrorCode(), data, null);
		
		ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CONFLICT);
		if (exception.getRetryAfterSeconds() != null) {
			builder.header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()));
		}
		
		return builder.body(response);
	}

	/**
	 * Handles registration pending error.
	 */
	@ExceptionHandler(RegistrationAlreadyPendingException.class)
	public ResponseEntity<ApiResponse<Object>> handleRegistrationAlreadyPendingException(RegistrationAlreadyPendingException exception) {
		log.warn("Registration Pending: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), exception.getErrorCode());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	/**
	 * Catch-all cho các lỗi server chưa được handle.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleException(Exception exception) {
		log.error("Unexpected error", exception);
		ApiResponse<Object> response = ApiResponse.error("Internal server error", "INTERNAL_SERVER_ERROR");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}
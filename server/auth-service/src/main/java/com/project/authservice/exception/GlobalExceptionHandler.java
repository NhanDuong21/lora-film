package com.project.authservice.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.authservice.common.ApiResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	/**
	 * Handles business rule violations.
	 *
	 * @param exception business exception
	 * @return error response
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException exception) {
		log.warn("Business exception: {}", exception.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(exception.getMessage()));
	}

	/**
	 * Handles unauthorized access.
	 *
	 * @param exception unauthorized exception
	 * @return error response
	 */
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException exception) {
		log.warn("Unauthorized: {}", exception.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(exception.getMessage()));
	}

	/**
	 * Handles missing resources.
	 *
	 * @param exception resource not found exception
	 * @return error response
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException exception) {
		log.warn("Resource not found: {}", exception.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(exception.getMessage()));
	}

	/**
	 * Handles validation errors from request body binding.
	 *
	 * @param exception validation exception
	 * @return error response
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getAllErrors().stream()
				.map(this::formatObjectError)
				.collect(Collectors.joining(", "));
		log.warn("Validation failed: {}", message);
		return ResponseEntity.badRequest().body(ApiResponse.failure(message));
	}

	/**
	 * Handles validation errors from parameter constraints.
	 *
	 * @param exception constraint violation exception
	 * @return error response
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException exception) {
		log.warn("Constraint violation: {}", exception.getMessage());
		return ResponseEntity.badRequest().body(ApiResponse.failure(exception.getMessage()));
	}

	/**
	 * Handles unexpected failures.
	 *
	 * @param exception unexpected exception
	 * @return error response
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleException(Exception exception) {
		log.error("Unexpected error", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.failure("Internal server error"));
	}

	private String formatObjectError(ObjectError objectError) {
		if (objectError instanceof org.springframework.validation.FieldError fieldError) {
			return fieldError.getField() + ": " + fieldError.getDefaultMessage();
		}
		return objectError.getDefaultMessage();
	}
}
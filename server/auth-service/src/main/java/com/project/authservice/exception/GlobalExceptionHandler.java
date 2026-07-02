package com.project.authservice.exception;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import com.project.authservice.exception.common.BusinessValidationException;
import com.project.authservice.exception.common.ExternalServiceUnavailableException;
import com.project.authservice.exception.common.GatewayTimeoutException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessValidationException.class)
	public ResponseEntity<ApiResponse<Object>> handleBusinessValidationException(BusinessValidationException exception) {
		log.warn("Business validation exception: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), exception.getErrorCode());
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
	}

	@ExceptionHandler(GatewayTimeoutException.class)
	public ResponseEntity<ApiResponse<Object>> handleGatewayTimeoutException(GatewayTimeoutException exception) {
		log.error("Gateway timeout: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Gateway timeout. Please try again later.", "GATEWAY_TIMEOUT");
		return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
	}

	@ExceptionHandler(ExternalServiceUnavailableException.class)
	public ResponseEntity<ApiResponse<Object>> handleExternalServiceUnavailableException(
			ExternalServiceUnavailableException exception) {
		log.error("External service unavailable: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Service is currently unavailable. Please try again later.", "SERVICE_UNAVAILABLE");
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	}

	@ExceptionHandler({
		org.springframework.data.redis.RedisConnectionFailureException.class,
		org.springframework.data.redis.RedisSystemException.class
	})
	public ResponseEntity<ApiResponse<Object>> handleRedisExceptions(Exception exception) {
		log.error("Redis infrastructure failure: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Service is currently unavailable. Please try again later.", "SERVICE_UNAVAILABLE");
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	}

	@ExceptionHandler({
		org.springframework.kafka.KafkaException.class,
		java.util.concurrent.TimeoutException.class
	})
	public ResponseEntity<ApiResponse<Object>> handleKafkaExceptions(Exception exception) {
		log.error("Kafka/Timeout infrastructure failure: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Gateway timeout. Please try again later.", "GATEWAY_TIMEOUT");
		return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
	}

	@ExceptionHandler(AccountNotVerifiedException.class)
	public ResponseEntity<ApiResponse<Object>> handleAccountNotVerifiedException(AccountNotVerifiedException exception) {
		log.warn("Auth Exception [{}]: {}", exception.getErrorCode(), exception.getMessage());
		java.util.Map<String, Long> data = new java.util.HashMap<>();
		data.put("accountId", exception.getAccountId());
		ApiResponse<Object> response = new ApiResponse<>(false, exception.getMessage(), exception.getErrorCode(), data, null);
		return ResponseEntity.status(exception.getStatus()).body(response);
	}

	@ExceptionHandler(OtpRateLimitException.class)
	public ResponseEntity<ApiResponse<Object>> handleOtpRateLimitException(OtpRateLimitException exception) {
		log.warn("Auth Exception [{}]: {}", exception.getErrorCode(), exception.getMessage());
		java.util.Map<String, Long> data = new java.util.HashMap<>();
		data.put("retryAfter", exception.getRetryAfter());
		ApiResponse<Object> response = new ApiResponse<>(false, exception.getMessage(), exception.getErrorCode(), data, null);
		return ResponseEntity.status(exception.getStatus()).body(response);
	}

	@ExceptionHandler(BaseAuthException.class)
	public ResponseEntity<ApiResponse<Object>> handleBaseAuthException(BaseAuthException exception) {
		log.warn("Auth Exception [{}]: {}", exception.getErrorCode(), exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), exception.getErrorCode());
		return ResponseEntity.status(exception.getStatus()).body(response);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException exception) {
		log.warn("Business exception: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), "BUSINESS_ERROR");
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException exception) {
		log.warn("Unauthorized: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), "UNAUTHORIZED");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException exception) {
		log.warn("Resource not found: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), "RESOURCE_NOT_FOUND");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException exception) {
		
		Map<String, List<FieldError>> fieldErrorsMap = exception.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.groupingBy(FieldError::getField));

		List<ApiResponse.ValidationError> validationErrors = fieldErrorsMap.entrySet().stream()
				.map(entry -> {
					FieldError highestPriorityError = entry.getValue().stream()
							.min(Comparator.comparingInt(this::getPriorityForFieldError))
							.orElse(entry.getValue().get(0));
					return new ApiResponse.ValidationError(entry.getKey(), highestPriorityError.getDefaultMessage());
				})
				.collect(Collectors.toList());

		log.warn("Validation failed: {} field errors", validationErrors.size());
		ApiResponse<Object> response = ApiResponse.validationError("Validation failed", validationErrors);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
			ConstraintViolationException exception) {
		
		Map<String, List<ConstraintViolation<?>>> violationsMap = exception.getConstraintViolations().stream()
				.collect(Collectors.groupingBy(v -> {
					String path = v.getPropertyPath().toString();
					return path.substring(path.lastIndexOf('.') + 1);
				}));

		List<ApiResponse.ValidationError> validationErrors = violationsMap.entrySet().stream()
				.map(entry -> {
					ConstraintViolation<?> highestPriorityViolation = entry.getValue().stream()
							.min(Comparator.comparingInt(this::getPriorityForViolation))
							.orElse(entry.getValue().get(0));
					return new ApiResponse.ValidationError(entry.getKey(), highestPriorityViolation.getMessage());
				})
				.collect(Collectors.toList());

		log.warn("Constraint violation: {} errors", validationErrors.size());
		ApiResponse<Object> response = ApiResponse.validationError("Validation failed", validationErrors);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
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
		ApiResponse<Object> response = ApiResponse.error("Invalid JSON format or unreadable request body", "MALFORMED_JSON");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Object>> handleHttpMediaTypeNotSupportedException(
			org.springframework.web.HttpMediaTypeNotSupportedException exception) {
		log.warn("Media type not supported: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Invalid Content-Type", "UNSUPPORTED_MEDIA_TYPE");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

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

	@ExceptionHandler(RegistrationAlreadyPendingException.class)
	public ResponseEntity<ApiResponse<Object>> handleRegistrationAlreadyPendingException(RegistrationAlreadyPendingException exception) {
		log.warn("Registration Pending: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage(), exception.getErrorCode());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleException(Exception exception) {
		log.error("Unexpected error", exception);
		ApiResponse<Object> response = ApiResponse.error("Internal server error", "INTERNAL_SERVER_ERROR");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	private int getPriorityForFieldError(FieldError fieldError) {
		String code = fieldError.getCode();
		return getPriorityLevel(code);
	}

	private int getPriorityForViolation(ConstraintViolation<?> violation) {
		String annotationName = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
		return getPriorityLevel(annotationName);
	}

	private int getPriorityLevel(String code) {
		if (code == null) return 5;
		switch (code) {
			case "NotNull":
			case "NotBlank":
			case "NotEmpty":
				return 1;
			case "Email":
			case "Pattern":
				return 2;
			case "Length":
			case "Size":
			case "Min":
			case "Max":
				return 3;
			default:
				return 4;
		}
	}
}
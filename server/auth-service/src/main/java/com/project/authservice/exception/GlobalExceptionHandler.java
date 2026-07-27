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
					return new ApiResponse.ValidationError(entry.getKey(), highestPriorityError.getCode(), highestPriorityError.getDefaultMessage());
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
					String code = highestPriorityViolation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
					return new ApiResponse.ValidationError(entry.getKey(), code, highestPriorityViolation.getMessage());
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
		
		ApiResponse<Object> response = new ApiResponse<>(false, exception.getMessage(), exception.getErrorCode(), data, exception.getErrors());
		
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

	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException exception) {
		log.warn("Access denied: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("You don't have permission to access this resource", "ACCESS_DENIED");
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
	}

	@ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
	public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(org.springframework.security.core.AuthenticationException exception) {
		log.warn("Authentication failed: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Authentication failed or token is invalid", "UNAUTHORIZED");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException exception) {
		log.warn("Missing request parameter: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Missing required parameter: " + exception.getParameterName(), "BAD_REQUEST");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception) {
		log.warn("Argument type mismatch: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Invalid parameter type for: " + exception.getName(), "BAD_REQUEST");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler({
		org.springframework.web.servlet.NoHandlerFoundException.class,
		org.springframework.web.servlet.resource.NoResourceFoundException.class
	})
	public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(Exception exception) {
		log.warn("No handler found: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Resource not found", "NOT_FOUND");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException exception) {
		log.warn("Data integrity violation: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Database conflict or constraint violation", "CONFLICT");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<ApiResponse<Object>> handleHttpMediaTypeNotAcceptableException(org.springframework.web.HttpMediaTypeNotAcceptableException exception) {
		log.warn("Media type not acceptable: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Accept header media type is not supported", "NOT_ACCEPTABLE");
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response);
	}

	@ExceptionHandler(org.springframework.web.bind.MissingPathVariableException.class)
	public ResponseEntity<ApiResponse<Object>> handleMissingPathVariableException(org.springframework.web.bind.MissingPathVariableException exception) {
		log.warn("Missing path variable: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Missing path variable: " + exception.getVariableName(), "BAD_REQUEST");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
	public ResponseEntity<ApiResponse<Object>> handleMissingRequestHeaderException(org.springframework.web.bind.MissingRequestHeaderException exception) {
		log.warn("Missing request header: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Missing required header: " + exception.getHeaderName(), "BAD_REQUEST");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
	public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestPartException(org.springframework.web.multipart.support.MissingServletRequestPartException exception) {
		log.warn("Missing request part: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Missing required request part: " + exception.getRequestPartName(), "BAD_REQUEST");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(org.springframework.validation.BindException.class)
	public ResponseEntity<ApiResponse<Object>> handleBindException(org.springframework.validation.BindException exception) {
		log.warn("Bind exception: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Request binding failed", "BAD_REQUEST");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(org.springframework.beans.TypeMismatchException.class)
	public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(org.springframework.beans.TypeMismatchException exception) {
		log.warn("Type mismatch: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Type mismatch for property: " + exception.getPropertyName(), "BAD_REQUEST");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotWritableException.class)
	public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotWritableException(org.springframework.http.converter.HttpMessageNotWritableException exception) {
		log.warn("Message not writable: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Could not write response", "INTERNAL_SERVER_ERROR");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	@ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException exception) {
		log.warn("Max upload size exceeded: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("File size exceeds maximum limit", "PAYLOAD_TOO_LARGE");
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
	}

	@ExceptionHandler({
		jakarta.persistence.EntityNotFoundException.class,
		org.springframework.orm.jpa.JpaObjectRetrievalFailureException.class,
		org.springframework.dao.EmptyResultDataAccessException.class
	})
	public ResponseEntity<ApiResponse<Object>> handleEntityNotFoundExceptions(Exception exception) {
		log.warn("Entity not found: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Requested data not found", "NOT_FOUND");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
	public ResponseEntity<ApiResponse<Object>> handleOptimisticLockingFailureException(org.springframework.dao.OptimisticLockingFailureException exception) {
		log.warn("Optimistic locking failure: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error("Data was modified by another transaction", "CONFLICT");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
	public ResponseEntity<ApiResponse<Object>> handleTransactionSystemException(org.springframework.transaction.TransactionSystemException exception) {
		log.warn("Transaction system exception: {}", exception.getMessage());
		Throwable rootCause = exception.getRootCause();
		if (rootCause instanceof jakarta.validation.ConstraintViolationException) {
			return handleConstraintViolationException((jakarta.validation.ConstraintViolationException) rootCause);
		}
		ApiResponse<Object> response = ApiResponse.error("Transaction failed", "INTERNAL_SERVER_ERROR");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException exception) {
		log.warn("Illegal argument: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage() != null ? exception.getMessage() : "Invalid argument provided", "BAD_REQUEST");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse<Object>> handleIllegalStateException(IllegalStateException exception) {
		log.warn("Illegal state: {}", exception.getMessage());
		ApiResponse<Object> response = ApiResponse.error(exception.getMessage() != null ? exception.getMessage() : "Invalid state for this operation", "CONFLICT");
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
package com.project.scoreservice.exception;
 
import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
 
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
 
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
 
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage(), ex.getErrorCode(), ex.getData(), null);
        return new ResponseEntity<>(response, ex.getStatus());
    }
 
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiResponse.ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiResponse.ValidationError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        boolean invalidRedeemPoints = ex.getBindingResult().getTarget() instanceof
                com.project.scoreservice.dto.RedeemPreviewRequest
                && ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getField)
                    .anyMatch("points"::equals);
        ApiResponse<Void> response = invalidRedeemPoints
                ? new ApiResponse<>(
                        false,
                        "Points must be greater than zero",
                        "SCORE_INVALID_POINT_AMOUNT",
                        null,
                        errors)
                : ApiResponse.validationError("Validation failed", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
 
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        ApiResponse<Void> response = ApiResponse.error("Invalid parameter format", "VALIDATION_ERROR");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
 
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ApiResponse<Void> response = ApiResponse.error("Invalid request payload", "VALIDATION_ERROR");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
 
    @ExceptionHandler({org.springframework.data.mapping.PropertyReferenceException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handlePropertyReferenceException(Exception ex) {
        ApiResponse<Void> response = ApiResponse.error("Invalid query parameter: " + ex.getMessage(), "SCORE_INVALID_QUERY");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        String errorId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        if (errorId == null || errorId.trim().isEmpty()) {
            errorId = UUID.randomUUID().toString();
        }
        log.error("Unhandled internal exception [Reference ID: {}]: ", errorId, ex);
        ApiResponse<Void> response = ApiResponse.error("An unexpected internal error occurred. Reference ID: " + errorId, "INTERNAL_SERVER_ERROR");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

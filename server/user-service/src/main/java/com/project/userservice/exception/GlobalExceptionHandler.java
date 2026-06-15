package com.project.userservice.exception;

import com.project.userservice.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        
        // Map specific error codes to HTTP status
        if ("USER_CCCD_ALREADY_EXISTS".equals(ex.getErrorCode()) || 
            "USER_PHONE_ALREADY_EXISTS".equals(ex.getErrorCode()) ||
            "USER_PROFILE_ALREADY_EXISTS".equals(ex.getErrorCode())) {
            status = HttpStatus.CONFLICT;
        } else if ("USER_NOT_FOUND".equals(ex.getErrorCode())) {
            status = HttpStatus.NOT_FOUND;
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", "INTERNAL_SERVER_ERROR"));
    }
}

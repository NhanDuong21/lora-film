package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class RegistrationConflictException extends BaseAuthException {
    private final Long retryAfterSeconds;
    private final java.util.List<com.project.authservice.common.ApiResponse.ValidationError> errors;

    public RegistrationConflictException(String message, String errorCode, Long retryAfterSeconds) {
        this(message, errorCode, retryAfterSeconds, null);
    }

    public RegistrationConflictException(String message, String errorCode, Long retryAfterSeconds, java.util.List<com.project.authservice.common.ApiResponse.ValidationError> errors) {
        super(message, errorCode, HttpStatus.CONFLICT); // 409 Conflict
        this.retryAfterSeconds = retryAfterSeconds;
        this.errors = errors;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public java.util.List<com.project.authservice.common.ApiResponse.ValidationError> getErrors() {
        return errors;
    }
}

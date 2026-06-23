package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class RegistrationConflictException extends BaseAuthException {
    private final Long retryAfterSeconds;

    public RegistrationConflictException(String message, String errorCode, Long retryAfterSeconds) {
        super(message, errorCode, HttpStatus.CONFLICT); // 409 Conflict
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

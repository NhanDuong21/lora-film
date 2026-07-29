package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class RegistrationAlreadyPendingException extends BaseAuthException {
    public RegistrationAlreadyPendingException(String message) {
        super(message, "REGISTRATION_ALREADY_PENDING", HttpStatus.CONFLICT); // 409 Conflict
    }
}

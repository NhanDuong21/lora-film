package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BaseAuthException {
    public EmailAlreadyExistsException() {
        super("Email already exists", "AUTH_EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT);
    }
}

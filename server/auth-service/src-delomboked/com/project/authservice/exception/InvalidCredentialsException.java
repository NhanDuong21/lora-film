package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BaseAuthException {
    public InvalidCredentialsException() {
        super("Invalid email or password", "AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
    }
}

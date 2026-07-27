package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BaseAuthException {
    public InvalidRefreshTokenException(String message) {
        super(message, "AUTH_INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
    }
}

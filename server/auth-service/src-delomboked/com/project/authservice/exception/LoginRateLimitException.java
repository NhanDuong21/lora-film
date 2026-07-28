package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class LoginRateLimitException extends BaseAuthException {
    public LoginRateLimitException() {
        super("Too many failed login attempts. Try again later.",
                "AUTH_LOGIN_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS);
    }
}

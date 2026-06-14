package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends BaseAuthException {
    public InvalidOtpException() {
        super("Invalid OTP code", "AUTH_INVALID_OTP", HttpStatus.BAD_REQUEST);
    }
}

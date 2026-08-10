package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class OtpNotFoundException extends BaseAuthException {
    public OtpNotFoundException() {
        super("No active OTP found. Please request a new OTP.", "OTP_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}

package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class VerificationExpiredException extends BaseAuthException {
    public VerificationExpiredException() {
        super("Verification OTP code has expired", "AUTH_VERIFICATION_EXPIRED", HttpStatus.BAD_REQUEST);
    }
}

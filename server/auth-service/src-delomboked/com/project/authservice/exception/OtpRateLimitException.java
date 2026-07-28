package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class OtpRateLimitException extends BaseAuthException {
    private final Long retryAfter;

    public OtpRateLimitException(Long retryAfter) {
        super("Please wait before requesting another OTP.", "OTP_RATE_LIMIT", HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
    }

    public Long getRetryAfter() {
        return retryAfter;
    }
}

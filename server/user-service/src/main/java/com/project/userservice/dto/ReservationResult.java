package com.project.userservice.dto;

public class ReservationResult {
    private final boolean success;
    private final String errorCode;
    private final Long retryAfterSeconds;

    public ReservationResult(boolean success, String errorCode, Long retryAfterSeconds) {
        this.success = success;
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

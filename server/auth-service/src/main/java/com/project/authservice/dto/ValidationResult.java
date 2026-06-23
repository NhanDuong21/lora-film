package com.project.authservice.dto;

public class ValidationResult {
    private String status;
    private String errorCode;
    private Long retryAfterSeconds;

    public ValidationResult() {}

    public ValidationResult(String status, String errorCode, Long retryAfterSeconds) {
        this.status = status;
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Long getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(Long retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; }
}

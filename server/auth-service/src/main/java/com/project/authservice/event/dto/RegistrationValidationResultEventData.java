package com.project.authservice.event.dto;

public class RegistrationValidationResultEventData {
    private String requestId;
    private String status; // SUCCESS or FAILED
    private String errorCode;
    private Long retryAfterSeconds;

    public RegistrationValidationResultEventData() {}

    public RegistrationValidationResultEventData(String requestId, String status, String errorCode, Long retryAfterSeconds) {
        this.requestId = requestId;
        this.status = status;
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Long getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(Long retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; }
}

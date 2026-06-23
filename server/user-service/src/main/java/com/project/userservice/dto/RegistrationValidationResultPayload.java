package com.project.userservice.dto;

public class RegistrationValidationResultPayload {
    private String requestId;
    private String status;
    private String errorCode;
    private Long retryAfterSeconds;

    public RegistrationValidationResultPayload(String requestId, String status, String errorCode, Long retryAfterSeconds) {
        this.requestId = requestId;
        this.status = status;
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getRequestId() { return requestId; }
    public String getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public Long getRetryAfterSeconds() { return retryAfterSeconds; }
}

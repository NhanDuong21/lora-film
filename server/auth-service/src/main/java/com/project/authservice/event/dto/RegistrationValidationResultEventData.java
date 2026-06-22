package com.project.authservice.event.dto;

public class RegistrationValidationResultEventData {
    private String requestId;
    private String status; // SUCCESS or FAILED
    private String errorCode;

    public RegistrationValidationResultEventData() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}

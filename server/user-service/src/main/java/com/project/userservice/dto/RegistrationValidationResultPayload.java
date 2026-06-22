package com.project.userservice.dto;

public class RegistrationValidationResultPayload {
    private String requestId;
    private String status;
    private String errorCode;

    public RegistrationValidationResultPayload(String requestId, String status, String errorCode) {
        this.requestId = requestId;
        this.status = status;
        this.errorCode = errorCode;
    }

    public String getRequestId() { return requestId; }
    public String getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}

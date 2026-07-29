package com.project.authservice.dto.response;

public class RegistrationInitiatedResponse {
    private String requestId;
    private String message;

    public RegistrationInitiatedResponse() {}

    public RegistrationInitiatedResponse(String requestId, String message) {
        this.requestId = requestId;
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

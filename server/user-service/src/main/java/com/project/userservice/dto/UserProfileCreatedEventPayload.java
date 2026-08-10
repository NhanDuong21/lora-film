package com.project.userservice.dto;

public class UserProfileCreatedEventPayload {
    private Long accountId;
    private String email;
    private String requestId;
    private String createdAt;
    private String status;

    public UserProfileCreatedEventPayload() {
    }

    public UserProfileCreatedEventPayload(Long accountId, String email, String requestId, String createdAt, String status) {
        this.accountId = accountId;
        this.email = email;
        this.requestId = requestId;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

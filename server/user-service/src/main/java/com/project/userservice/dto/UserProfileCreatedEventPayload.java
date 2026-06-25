package com.project.userservice.dto;

public class UserProfileCreatedEventPayload {
    private Long accountId;
    private String requestId;
    private String createdAt;

    public UserProfileCreatedEventPayload() {
    }

    public UserProfileCreatedEventPayload(Long accountId, String requestId, String createdAt) {
        this.accountId = accountId;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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
}

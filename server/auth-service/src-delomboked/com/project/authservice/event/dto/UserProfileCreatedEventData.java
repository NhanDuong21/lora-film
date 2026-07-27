package com.project.authservice.event.dto;


public class UserProfileCreatedEventData {
    private Long accountId;
    private String email;
    private String requestId;
    private String createdAt;
    private String status;
    public Long getAccountId() {
        return this.accountId;
    }
    public String getEmail() {
        return this.email;
    }
    public String getRequestId() {
        return this.requestId;
    }
    public String getCreatedAt() {
        return this.createdAt;
    }
    public String getStatus() {
        return this.status;
    }
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public UserProfileCreatedEventData() {
    }
    public UserProfileCreatedEventData(Long accountId, String email, String requestId, String createdAt, String status) {
        this.accountId = accountId;
        this.email = email;
        this.requestId = requestId;
        this.createdAt = createdAt;
        this.status = status;
    }
}

package com.project.authservice.entity;

import java.time.LocalDateTime;

public class RedisOtpData {
    private String otpHash;
    private int failedAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime lastSentAt;

    public RedisOtpData() {}

    public RedisOtpData(String otpHash, int failedAttempts, LocalDateTime createdAt, LocalDateTime lastSentAt) {
        this.otpHash = otpHash;
        this.failedAttempts = failedAttempts;
        this.createdAt = createdAt;
        this.lastSentAt = lastSentAt;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(LocalDateTime lastSentAt) {
        this.lastSentAt = lastSentAt;
    }
}

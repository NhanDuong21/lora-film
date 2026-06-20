package com.project.authservice.entity;

public class RedisOtpData {
    private String otpHash;
    private int failedAttempts;
    private long createdAt;
    private long lastSentAt;

    public RedisOtpData() {}

    public RedisOtpData(String otpHash, int failedAttempts, long createdAt, long lastSentAt) {
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(long lastSentAt) {
        this.lastSentAt = lastSentAt;
    }
}

package com.project.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "access_token_hash", nullable = false)
    private String accessTokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refresh_token_id")
    private RefreshToken refreshToken;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "device_name")
    private String deviceName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    .Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    public String getId() {
        return this.id;
    }
    public Account getAccount() {
        return this.account;
    }
    public String getAccessTokenHash() {
        return this.accessTokenHash;
    }
    public RefreshToken getRefreshToken() {
        return this.refreshToken;
    }
    public String getIpAddress() {
        return this.ipAddress;
    }
    public String getUserAgent() {
        return this.userAgent;
    }
    public String getDeviceName() {
        return this.deviceName;
    }
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public LocalDateTime getLastActiveAt() {
        return this.lastActiveAt;
    }
    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }
    public Boolean isIsActive() {
        return this.isActive;
    }
    public void setId(String id) {
        this.id = id;
    }
    public void setAccount(Account account) {
        this.account = account;
    }
    public void setAccessTokenHash(String accessTokenHash) {
        this.accessTokenHash = accessTokenHash;
    }
    public void setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    public UserSession() {
    }
    public UserSession(String id, Account account, String accessTokenHash, RefreshToken refreshToken, String ipAddress, String userAgent, String deviceName, LocalDateTime createdAt, LocalDateTime lastActiveAt, LocalDateTime expiresAt, Boolean isActive) {
        this.id = id;
        this.account = account;
        this.accessTokenHash = accessTokenHash;
        this.refreshToken = refreshToken;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceName = deviceName;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.expiresAt = expiresAt;
        this.isActive = isActive;
    }
    public static UserSessionBuilder builder() {
        return new UserSessionBuilder();
    }
    public static class UserSessionBuilder {
        private String id;
        private Account account;
        private String accessTokenHash;
        private RefreshToken refreshToken;
        private String ipAddress;
        private String userAgent;
        private String deviceName;
        private LocalDateTime createdAt;
        private LocalDateTime lastActiveAt;
        private LocalDateTime expiresAt;
        private Boolean isActive;
        UserSessionBuilder() {}
        public UserSessionBuilder id(String id) {
            this.id = id;
            return this;
        }
        public UserSessionBuilder account(Account account) {
            this.account = account;
            return this;
        }
        public UserSessionBuilder accessTokenHash(String accessTokenHash) {
            this.accessTokenHash = accessTokenHash;
            return this;
        }
        public UserSessionBuilder refreshToken(RefreshToken refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }
        public UserSessionBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        public UserSessionBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        public UserSessionBuilder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }
        public UserSessionBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public UserSessionBuilder lastActiveAt(LocalDateTime lastActiveAt) {
            this.lastActiveAt = lastActiveAt;
            return this;
        }
        public UserSessionBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        public UserSessionBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }
        public UserSession build() {
            return new UserSession(this.id, this.account, this.accessTokenHash, this.refreshToken, this.ipAddress, this.userAgent, this.deviceName, this.createdAt, this.lastActiveAt, this.expiresAt, this.isActive);
        }
    }
}

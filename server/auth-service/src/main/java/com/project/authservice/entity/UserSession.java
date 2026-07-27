package com.project.authservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refresh_token_id")
    private RefreshToken refreshToken;

    @Column(name = "device_name", length = 150)
    private String deviceName;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "operating_system", length = 100)
    private String operatingSystem;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt = LocalDateTime.now();

    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt = LocalDateTime.now();

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = true;

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public void setId(String id) {} // Ignored compatibility

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public RefreshToken getRefreshToken() { return refreshToken; }
    public void setRefreshToken(RefreshToken refreshToken) { this.refreshToken = refreshToken; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getLoginAt() { return loginAt; }
    public void setLoginAt(LocalDateTime loginAt) { this.loginAt = loginAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }

    public LocalDateTime getLogoutAt() { return logoutAt; }
    public void setLogoutAt(LocalDateTime logoutAt) { this.logoutAt = logoutAt; }

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }

    // --- Backwards Compatibility Aliases ---
    public LocalDateTime getCreatedAt() { return loginAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.loginAt = createdAt != null ? createdAt : LocalDateTime.now(); }

    public LocalDateTime getExpiresAt() { return expiredAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiredAt = expiresAt; }

    public Boolean getIsActive() { return isOnline; }
    public void setIsActive(Boolean isActive) { this.isOnline = isActive; }
    
    public String getAccessTokenHash() { return null; }
    public void setAccessTokenHash(String hash) {}

    public UserSession() {}

    public UserSession(Long id, Account account, RefreshToken refreshToken, String deviceName, String deviceType, String browser, String operatingSystem, String ipAddress, String userAgent, LocalDateTime loginAt, LocalDateTime lastActiveAt, LocalDateTime expiredAt, LocalDateTime logoutAt, Boolean isOnline) {
        this.id = id;
        this.account = account;
        this.refreshToken = refreshToken;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.browser = browser;
        this.operatingSystem = operatingSystem;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.loginAt = loginAt;
        this.lastActiveAt = lastActiveAt;
        this.expiredAt = expiredAt;
        this.logoutAt = logoutAt;
        this.isOnline = isOnline;
    }

    public static UserSessionBuilder builder() {
        return new UserSessionBuilder();
    }

    public static class UserSessionBuilder {
        private Long id;
        private Account account;
        private RefreshToken refreshToken;
        private String deviceName;
        private String deviceType;
        private String browser;
        private String operatingSystem;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime loginAt;
        private LocalDateTime lastActiveAt;
        private LocalDateTime expiredAt;
        private LocalDateTime logoutAt;
        private Boolean isOnline;

        UserSessionBuilder() {}

        public UserSessionBuilder id(Long id) { this.id = id; return this; }
        public UserSessionBuilder id(String id) { return this; } // Ignored compatibility
        public UserSessionBuilder account(Account account) { this.account = account; return this; }
        public UserSessionBuilder refreshToken(RefreshToken refreshToken) { this.refreshToken = refreshToken; return this; }
        public UserSessionBuilder deviceName(String deviceName) { this.deviceName = deviceName; return this; }
        public UserSessionBuilder deviceType(String deviceType) { this.deviceType = deviceType; return this; }
        public UserSessionBuilder browser(String browser) { this.browser = browser; return this; }
        public UserSessionBuilder operatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; return this; }
        public UserSessionBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public UserSessionBuilder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public UserSessionBuilder loginAt(LocalDateTime loginAt) { this.loginAt = loginAt; return this; }
        public UserSessionBuilder lastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; return this; }
        public UserSessionBuilder expiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; return this; }
        public UserSessionBuilder logoutAt(LocalDateTime logoutAt) { this.logoutAt = logoutAt; return this; }
        public UserSessionBuilder isOnline(Boolean isOnline) { this.isOnline = isOnline; return this; }
        
        // Aliases
        public UserSessionBuilder createdAt(LocalDateTime createdAt) { this.loginAt = createdAt; return this; }
        public UserSessionBuilder expiresAt(LocalDateTime expiresAt) { this.expiredAt = expiresAt; return this; }
        public UserSessionBuilder isActive(Boolean isActive) { this.isOnline = isActive; return this; }
        public UserSessionBuilder accessTokenHash(String hash) { return this; }

        public UserSession build() {
            if (this.loginAt == null) this.loginAt = LocalDateTime.now();
            if (this.lastActiveAt == null) this.lastActiveAt = LocalDateTime.now();
            if (this.isOnline == null) this.isOnline = true;
            return new UserSession(this.id, this.account, this.refreshToken, this.deviceName, this.deviceType, this.browser, this.operatingSystem, this.ipAddress, this.userAgent, this.loginAt, this.lastActiveAt, this.expiredAt, this.logoutAt, this.isOnline);
        }
    }
}

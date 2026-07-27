package com.project.authservice.dto;


import java.time.LocalDateTime;

public class SessionDto {
    private Long id;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    public Long getId() {
        return this.id;
    }
    public String getIpAddress() {
        return this.ipAddress;
    }
    public String getUserAgent() {
        return this.userAgent;
    }
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }
    public Boolean getIsActive() {
        return this.isActive;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    public SessionDto() {
    }
    public SessionDto(Long id, String ipAddress, String userAgent, LocalDateTime createdAt, LocalDateTime expiresAt, Boolean isActive) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isActive = isActive;
    }
    public static SessionDtoBuilder builder() {
        return new SessionDtoBuilder();
    }
    public static class SessionDtoBuilder {
        private Long id;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private Boolean isActive;
        SessionDtoBuilder() {}
        public SessionDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public SessionDtoBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        public SessionDtoBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        public SessionDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public SessionDtoBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        public SessionDtoBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }
        public SessionDto build() {
            return new SessionDto(this.id, this.ipAddress, this.userAgent, this.createdAt, this.expiresAt, this.isActive);
        }
    }
}

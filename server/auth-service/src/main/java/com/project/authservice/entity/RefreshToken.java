package com.project.authservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
	private String token;

	@Column(name = "device_id", length = 120)
	private String deviceId;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiryDate;

	@Column(name = "revoked", nullable = false)
	private Boolean isRevoked = false;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (isRevoked == null) {
			isRevoked = false;
		}
	}

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public Account getAccount() { return this.account; }
    public void setAccount(Account account) { this.account = account; }

    public String getToken() { return this.token; }
    public void setToken(String token) { this.token = token; }

    public String getDeviceId() { return this.deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public LocalDateTime getExpiryDate() { return this.expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    
    public LocalDateTime getExpiresAt() { return this.expiryDate; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiryDate = expiresAt; }

    public Boolean getIsRevoked() { return this.isRevoked; }
    public void setIsRevoked(Boolean isRevoked) { this.isRevoked = isRevoked; }

    public LocalDateTime getRevokedAt() { return this.revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public RefreshToken() {}

    public RefreshToken(Long id, Account account, String token, String deviceId, LocalDateTime expiryDate, Boolean isRevoked, LocalDateTime revokedAt, LocalDateTime createdAt) {
        this.id = id;
        this.account = account;
        this.token = token;
        this.deviceId = deviceId;
        this.expiryDate = expiryDate;
        this.isRevoked = isRevoked;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    public static RefreshTokenBuilder builder() {
        return new RefreshTokenBuilder();
    }

    public static class RefreshTokenBuilder {
        private Long id;
        private Account account;
        private String token;
        private String deviceId;
        private LocalDateTime expiryDate;
        private Boolean isRevoked;
        private LocalDateTime revokedAt;
        private LocalDateTime createdAt;
        RefreshTokenBuilder() {}
        public RefreshTokenBuilder id(Long id) { this.id = id; return this; }
        public RefreshTokenBuilder account(Account account) { this.account = account; return this; }
        public RefreshTokenBuilder token(String token) { this.token = token; return this; }
        public RefreshTokenBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public RefreshTokenBuilder expiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; return this; }
        public RefreshTokenBuilder isRevoked(Boolean isRevoked) { this.isRevoked = isRevoked; return this; }
        public RefreshTokenBuilder revokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; return this; }
        public RefreshTokenBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RefreshToken build() {
            return new RefreshToken(this.id, this.account, this.token, this.deviceId, this.expiryDate, this.isRevoked, this.revokedAt, this.createdAt);
        }
    }
}

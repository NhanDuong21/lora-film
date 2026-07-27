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

	@Column(name = "token_hash", nullable = false, unique = true, length = 255)
	private String token;

	@Column(name = "expiry_date", nullable = false)
	private LocalDateTime expiryDate;

	@Column(name = "is_revoked", nullable = false)
	private Boolean isRevoked = false;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "updated_by")
	private Long updatedBy;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (isRevoked == null) {
			isRevoked = false;
		}
	}
    public Long getId() {
        return this.id;
    }
    public Account getAccount() {
        return this.account;
    }
    public String getToken() {
        return this.token;
    }
    public LocalDateTime getExpiryDate() {
        return this.expiryDate;
    }
    public Boolean getIsRevoked() {
        return this.isRevoked;
    }
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public Long getCreatedBy() {
        return this.createdBy;
    }
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    public Long getUpdatedBy() {
        return this.updatedBy;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setAccount(Account account) {
        this.account = account;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
    public void setIsRevoked(Boolean isRevoked) {
        this.isRevoked = isRevoked;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
    public RefreshToken() {
    }
    public RefreshToken(Long id, Account account, String token, LocalDateTime expiryDate, Boolean isRevoked, LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy) {
        this.id = id;
        this.account = account;
        this.token = token;
        this.expiryDate = expiryDate;
        this.isRevoked = isRevoked;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
    public static RefreshTokenBuilder builder() {
        return new RefreshTokenBuilder();
    }
    public static class RefreshTokenBuilder {
        private Long id;
        private Account account;
        private String token;
        private LocalDateTime expiryDate;
        private Boolean isRevoked;
        private LocalDateTime createdAt;
        private Long createdBy;
        private LocalDateTime updatedAt;
        private Long updatedBy;
        RefreshTokenBuilder() {}
        public RefreshTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public RefreshTokenBuilder account(Account account) {
            this.account = account;
            return this;
        }
        public RefreshTokenBuilder token(String token) {
            this.token = token;
            return this;
        }
        public RefreshTokenBuilder expiryDate(LocalDateTime expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }
        public RefreshTokenBuilder isRevoked(Boolean isRevoked) {
            this.isRevoked = isRevoked;
            return this;
        }
        public RefreshTokenBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public RefreshTokenBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        public RefreshTokenBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public RefreshTokenBuilder updatedBy(Long updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        public RefreshToken build() {
            return new RefreshToken(this.id, this.account, this.token, this.expiryDate, this.isRevoked, this.createdAt, this.createdBy, this.updatedAt, this.updatedBy);
        }
    }
}

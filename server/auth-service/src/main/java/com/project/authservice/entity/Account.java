package com.project.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.project.authservice.enums.AccountStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false, unique = true, length = 100)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_status", length = 20)
	private AccountStatus accountStatus = AccountStatus.PENDING;

	@Version
	@Column(name = "version")
	private Integer version = 0;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "updated_by")
	private Long updatedBy;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
		if (accountStatus == null) {
			accountStatus = AccountStatus.PENDING;
		}
		if (version == null) {
			version = 0;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
    public Long getId() {
        return this.id;
    }
    public String getEmail() {
        return this.email;
    }
    public String getPasswordHash() {
        return this.passwordHash;
    }
    public Role getRole() {
        return this.role;
    }
    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }
    public Integer getVersion() {
        return this.version;
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
    public Boolean getIsDeleted() {
        return this.isDeleted;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    public void setRole(Role role) {
        this.role = role;
    }
    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }
    public void setVersion(Integer version) {
        this.version = version;
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
    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
    public Account() {
    }
    public Account(Long id, String email, String passwordHash, Role role, AccountStatus accountStatus, Integer version, LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy, Boolean isDeleted) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.accountStatus = accountStatus;
        this.version = version;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.isDeleted = isDeleted;
    }
    public static AccountBuilder builder() {
        return new AccountBuilder();
    }
    public static class AccountBuilder {
        private Long id;
        private String email;
        private String passwordHash;
        private Role role;
        private AccountStatus accountStatus;
        private Integer version;
        private LocalDateTime createdAt;
        private Long createdBy;
        private LocalDateTime updatedAt;
        private Long updatedBy;
        private Boolean isDeleted;
        AccountBuilder() {}
        public AccountBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public AccountBuilder email(String email) {
            this.email = email;
            return this;
        }
        public AccountBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }
        public AccountBuilder role(Role role) {
            this.role = role;
            return this;
        }
        public AccountBuilder accountStatus(AccountStatus accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }
        public AccountBuilder version(Integer version) {
            this.version = version;
            return this;
        }
        public AccountBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public AccountBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        public AccountBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public AccountBuilder updatedBy(Long updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        public AccountBuilder isDeleted(Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }
        public Account build() {
            return new Account(this.id, this.email, this.passwordHash, this.role, this.accountStatus, this.version, this.createdAt, this.createdBy, this.updatedAt, this.updatedBy, this.isDeleted);
        }
    }
}
package com.project.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.project.authservice.enums.AccountStatus;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "accounts")
@jakarta.persistence.EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private AccountStatus status = AccountStatus.INACTIVE;

	@Column(name = "is_enabled", nullable = false)
	private Boolean isEnabled = true;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@org.springframework.data.annotation.CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@org.springframework.data.annotation.CreatedBy
	@Column(name = "created_by")
	private Long createdBy;

	@org.springframework.data.annotation.LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@org.springframework.data.annotation.LastModifiedBy
	@Column(name = "updated_by")
	private Long updatedBy;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "account_roles",
			joinColumns = @JoinColumn(name = "account_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	private Set<Role> roles = new HashSet<>();

	@PrePersist
	void prePersist() {
		if (status == null) {
			status = AccountStatus.INACTIVE;
		}
		if (isEnabled == null) {
			isEnabled = true;
		}
		if (isDeleted == null) {
			isDeleted = false;
		}
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPasswordHash() { return passwordHash; }
	public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

	public AccountStatus getAccountStatus() { return status; }
	public void setAccountStatus(AccountStatus status) { this.status = status; }

	public AccountStatus getStatus() { return status; }
	public void setStatus(AccountStatus status) { this.status = status; }

	public Boolean getIsEnabled() { return isEnabled; }
	public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

	public Boolean getIsDeleted() { return isDeleted; }
	public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

	public LocalDateTime getLastLoginAt() { return lastLoginAt; }
	public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

	public Long getCreatedBy() { return createdBy; }
	public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

	public Long getUpdatedBy() { return updatedBy; }
	public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

	public Set<Role> getRoles() { return roles; }
	public void setRoles(Set<Role> roles) { this.roles = roles; }

	public Role getRole() {
		if (roles != null && !roles.isEmpty()) {
			return roles.iterator().next();
		}
		return null;
	}

	public void setRole(Role role) {
		if (this.roles == null) {
			this.roles = new HashSet<>();
		}
		this.roles.clear();
		if (role != null) {
			this.roles.add(role);
		}
	}

	public Account() {}

	public Account(Long id, String email, String passwordHash, AccountStatus status, Boolean isEnabled, Boolean isDeleted, LocalDateTime lastLoginAt, LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy, Set<Role> roles) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.status = status;
		this.isEnabled = isEnabled;
		this.isDeleted = isDeleted;
		this.lastLoginAt = lastLoginAt;
		this.createdAt = createdAt;
		this.createdBy = createdBy;
		this.updatedAt = updatedAt;
		this.updatedBy = updatedBy;
		this.roles = roles;
	}

    public static AccountBuilder builder() {
        return new AccountBuilder();
    }

    public static class AccountBuilder {
        private Long id;
        private String email;
        private String passwordHash;
        private AccountStatus status;
        private Boolean isEnabled;
        private Boolean isDeleted;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
        private Long createdBy;
        private LocalDateTime updatedAt;
        private Long updatedBy;
        private Set<Role> roles;

        AccountBuilder() {}

        public AccountBuilder id(Long id) { this.id = id; return this; }
        public AccountBuilder email(String email) { this.email = email; return this; }
        public AccountBuilder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public AccountBuilder status(AccountStatus status) { this.status = status; return this; }
        public AccountBuilder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public AccountBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public AccountBuilder lastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; return this; }
        public AccountBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AccountBuilder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public AccountBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public AccountBuilder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public AccountBuilder roles(Set<Role> roles) { this.roles = roles; return this; }

        public Account build() {
            return new Account(id, email, passwordHash, status, isEnabled, isDeleted, lastLoginAt, createdAt, createdBy, updatedAt, updatedBy, roles);
        }
    }
}
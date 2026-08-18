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
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	private Account account;

	@Column(name = "action", nullable = false, length = 100)
	private String action;

	@Column(name = "resource", nullable = false, length = 100)
	private String resource;

	@Column(name = "resource_id", length = 100)
	private String resourceId;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "result", nullable = false, length = 20)
	private String result = "SUCCESS";

	@Column(name = "severity", nullable = false, length = 20)
	private String severity = "NORMAL";

	@Column(name = "review_status", nullable = false, length = 20)
	private String reviewStatus = "NOT_REQUIRED";

	@Column(name = "reviewed_by")
	private Long reviewedBy;

	@Column(name = "review_note", length = 500)
	private String reviewNote;

	@Column(name = "reviewed_at")
	private LocalDateTime reviewedAt;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

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
	}
    public Long getId() {
        return this.id;
    }
    public Account getAccount() {
        return this.account;
    }
    public String getAction() {
        return this.action;
    }
    public String getResource() {
        return this.resource;
    }
    public String getResourceId() {
        return this.resourceId;
    }
    public String getDescription() {
        return this.description;
    }
    public String getResult() { return result; }
    public String getSeverity() { return severity; }
    public String getReviewStatus() { return reviewStatus; }
    public Long getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getIpAddress() {
        return this.ipAddress;
    }
    public String getUserAgent() {
        return this.userAgent;
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
    public void setAction(String action) {
        this.action = action;
    }
    public void setResource(String resource) {
        this.resource = resource;
    }
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setResult(String result) { this.result = result; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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
    public AuditLog() {
    }
    public AuditLog(Long id, Account account, String action, String resource, String resourceId, String description, String ipAddress, String userAgent, LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy) {
        this.id = id;
        this.account = account;
        this.action = action;
        this.resource = resource;
        this.resourceId = resourceId;
        this.description = description;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }
    public static class AuditLogBuilder {
        private Long id;
        private Account account;
        private String action;
        private String resource;
        private String resourceId;
        private String description;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime createdAt;
        private Long createdBy;
        private LocalDateTime updatedAt;
        private Long updatedBy;
        AuditLogBuilder() {}
        public AuditLogBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public AuditLogBuilder account(Account account) {
            this.account = account;
            return this;
        }
        public AuditLogBuilder action(String action) {
            this.action = action;
            return this;
        }
        public AuditLogBuilder resource(String resource) {
            this.resource = resource;
            return this;
        }
        public AuditLogBuilder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }
        public AuditLogBuilder description(String description) {
            this.description = description;
            return this;
        }
        public AuditLogBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        public AuditLogBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        public AuditLogBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public AuditLogBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        public AuditLogBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public AuditLogBuilder updatedBy(Long updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        public AuditLog build() {
            return new AuditLog(id, account, action, resource, resourceId, description, ipAddress, userAgent, createdAt, createdBy, updatedAt, updatedBy);
        }
    }
}

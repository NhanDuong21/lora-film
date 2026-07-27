package com.project.authservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class Permission {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "permission_code", nullable = false, unique = true, length = 100)
	private String permissionCode;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "updated_by")
	private Long updatedBy;
    public Integer getId() {
        return this.id;
    }
    public String getPermissionCode() {
        return this.permissionCode;
    }
    public String getDescription() {
        return this.description;
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
    public void setId(Integer id) {
        this.id = id;
    }
    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
    public void setDescription(String description) {
        this.description = description;
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
    public Permission() {
    }
    public Permission(Integer id, String permissionCode, String description, LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy) {
        this.id = id;
        this.permissionCode = permissionCode;
        this.description = description;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
    public static PermissionBuilder builder() {
        return new PermissionBuilder();
    }
    public static class PermissionBuilder {
        private Integer id;
        private String permissionCode;
        private String description;
        private LocalDateTime createdAt;
        private Long createdBy;
        private LocalDateTime updatedAt;
        private Long updatedBy;
        PermissionBuilder() {}
        public PermissionBuilder id(Integer id) {
            this.id = id;
            return this;
        }
        public PermissionBuilder permissionCode(String permissionCode) {
            this.permissionCode = permissionCode;
            return this;
        }
        public PermissionBuilder description(String description) {
            this.description = description;
            return this;
        }
        public PermissionBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public PermissionBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        public PermissionBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public PermissionBuilder updatedBy(Long updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        public Permission build() {
            return new Permission(this.id, this.permissionCode, this.description, this.createdAt, this.createdBy, this.updatedAt, this.updatedBy);
        }
    }
}

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
	private Long id;

	@Column(name = "code", nullable = false, unique = true, length = 100)
	private String code;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "module", nullable = false, length = 100)
	private String module;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return this.code; }
    public void setCode(String code) { this.code = code; }

    public String getPermissionCode() { return this.code; }
    public void setPermissionCode(String permissionCode) { this.code = permissionCode; }

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }

    public String getModule() { return this.module; }
    public void setModule(String module) { this.module = module; }

    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Permission() {}

    public Permission(Long id, String code, String name, String module, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.module = module;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PermissionBuilder builder() {
        return new PermissionBuilder();
    }

    public static class PermissionBuilder {
        private Long id;
        private String code;
        private String name;
        private String module;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        PermissionBuilder() {}
        public PermissionBuilder id(Long id) { this.id = id; return this; }
        public PermissionBuilder code(String code) { this.code = code; return this; }
        public PermissionBuilder permissionCode(String permissionCode) { this.code = permissionCode; return this; }
        public PermissionBuilder name(String name) { this.name = name; return this; }
        public PermissionBuilder module(String module) { this.module = module; return this; }
        public PermissionBuilder description(String description) { this.description = description; return this; }
        public PermissionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PermissionBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Permission build() {
            return new Permission(this.id, this.code, this.name, this.module, this.description, this.createdAt, this.updatedAt);
        }
    }
}

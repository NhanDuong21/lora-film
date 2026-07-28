package com.project.authservice.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "role_name", nullable = false, unique = true, length = 50)
	private String roleName;

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

	.Default
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "roles_permissions",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "permission_id")
	)
	private Set<Permission> permissions = new HashSet<>();
    public Integer getId() {
        return this.id;
    }
    public String getRoleName() {
        return this.roleName;
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
    public Set<Permission> getPermissions() {
        return this.permissions;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public void setRoleName(String roleName) {
        this.roleName = roleName;
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
    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
    public Role() {
    }
    public Role(Integer id, String roleName, String description, LocalDateTime createdAt, Long createdBy, LocalDateTime updatedAt, Long updatedBy, Set<Permission> permissions) {
        this.id = id;
        this.roleName = roleName;
        this.description = description;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.permissions = permissions;
    }
    public static RoleBuilder builder() {
        return new RoleBuilder();
    }
    public static class RoleBuilder {
        private Integer id;
        private String roleName;
        private String description;
        private LocalDateTime createdAt;
        private Long createdBy;
        private LocalDateTime updatedAt;
        private Long updatedBy;
        private Set<Permission> permissions;
        RoleBuilder() {}
        public RoleBuilder id(Integer id) {
            this.id = id;
            return this;
        }
        public RoleBuilder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }
        public RoleBuilder description(String description) {
            this.description = description;
            return this;
        }
        public RoleBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public RoleBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        public RoleBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public RoleBuilder updatedBy(Long updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        public RoleBuilder permissions(Set<Permission> permissions) {
            this.permissions = permissions;
            return this;
        }
        public Role build() {
            return new Role(this.id, this.roleName, this.description, this.createdAt, this.createdBy, this.updatedAt, this.updatedBy, this.permissions);
        }
    }
}
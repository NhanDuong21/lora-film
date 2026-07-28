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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

@Entity
@Table(name = "roles")
@EntityListeners(AuditingEntityListener.class)
public class Role {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "code", nullable = false, unique = true, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String roleName;

	@Column(name = "description", length = 255)
	private String description;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "roles_permissions",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "permission_id")
	)
	private Set<Permission> permissions = new HashSet<>();

    public Long getId() {
        return this.id;
    }
    public String getCode() {
        return this.code;
    }
    public String getName() {
        return this.roleName;
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
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    public Set<Permission> getPermissions() {
        return this.permissions;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public void setName(String name) {
        this.roleName = name;
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
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
    public Role() {
    }
    public Role(Long id, String code, String roleName, String description, LocalDateTime createdAt, LocalDateTime updatedAt, Set<Permission> permissions) {
        this.id = id;
        this.code = code;
        this.roleName = roleName;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.permissions = permissions;
    }
    public static RoleBuilder builder() {
        return new RoleBuilder();
    }
    public static class RoleBuilder {
        private Long id;
        private String code;
        private String roleName;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Set<Permission> permissions;
        RoleBuilder() {}
        public RoleBuilder id(Long id) { this.id = id; return this; }
        public RoleBuilder code(String code) { this.code = code; return this; }
        public RoleBuilder roleName(String roleName) { this.roleName = roleName; return this; }
        public RoleBuilder description(String description) { this.description = description; return this; }
        public RoleBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RoleBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public RoleBuilder permissions(Set<Permission> permissions) { this.permissions = permissions; return this; }
        public Role build() {
            return new Role(this.id, this.code, this.roleName, this.description, this.createdAt, this.updatedAt, this.permissions);
        }
    }
}
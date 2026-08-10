package com.project.authservice.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles_permissions")
public class RolePermission {

	@EmbeddedId
	private RolePermissionId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("roleId")
	@JoinColumn(name = "role_id")
	private Role role;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("permissionId")
	@JoinColumn(name = "permission_id")
	private Permission permission;
    public RolePermissionId getId() {
        return this.id;
    }
    public Role getRole() {
        return this.role;
    }
    public Permission getPermission() {
        return this.permission;
    }
    public void setId(RolePermissionId id) {
        this.id = id;
    }
    public void setRole(Role role) {
        this.role = role;
    }
    public void setPermission(Permission permission) {
        this.permission = permission;
    }
    public RolePermission() {
    }
    public RolePermission(RolePermissionId id, Role role, Permission permission) {
        this.id = id;
        this.role = role;
        this.permission = permission;
    }
    public static RolePermissionBuilder builder() {
        return new RolePermissionBuilder();
    }
    public static class RolePermissionBuilder {
        private RolePermissionId id;
        private Role role;
        private Permission permission;
        RolePermissionBuilder() {}
        public RolePermissionBuilder id(RolePermissionId id) {
            this.id = id;
            return this;
        }
        public RolePermissionBuilder role(Role role) {
            this.role = role;
            return this;
        }
        public RolePermissionBuilder permission(Permission permission) {
            this.permission = permission;
            return this;
        }
        public RolePermission build() {
            return new RolePermission(this.id, this.role, this.permission);
        }
    }
}

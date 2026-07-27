package com.project.authservice.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RolePermissionId implements Serializable {

	@Column(name = "role_id")
	private Long roleId;

	@Column(name = "permission_id")
	private Long permissionId;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RolePermissionId that = (RolePermissionId) o;
		return Objects.equals(roleId, that.roleId) && Objects.equals(permissionId, that.permissionId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(roleId, permissionId);
	}
    public Long getRoleId() {
        return this.roleId;
    }
    public Long getPermissionId() {
        return this.permissionId;
    }
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }
    public RolePermissionId() {
    }
    public RolePermissionId(Long roleId, Long permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }
}

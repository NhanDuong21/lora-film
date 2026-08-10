package com.project.authservice.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RolePermissionId implements Serializable {

	@Column(name = "role_id")
	private Integer roleId;

	@Column(name = "permission_id")
	private Integer permissionId;

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
    public Integer getRoleId() {
        return this.roleId;
    }
    public Integer getPermissionId() {
        return this.permissionId;
    }
    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }
    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
    }
    public RolePermissionId() {
    }
    public RolePermissionId(Integer roleId, Integer permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }
}

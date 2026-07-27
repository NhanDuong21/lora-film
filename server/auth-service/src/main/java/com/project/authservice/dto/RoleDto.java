package com.project.authservice.dto;


import java.util.Set;

public class RoleDto {
    private Long id;
    private String roleName;
    private String description;
    private Set<PermissionDto> permissions;
    public Long getId() {
        return this.id;
    }
    public String getRoleName() {
        return this.roleName;
    }
    public String getDescription() {
        return this.description;
    }
    public Set<PermissionDto> getPermissions() {
        return this.permissions;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setPermissions(Set<PermissionDto> permissions) {
        this.permissions = permissions;
    }
    public RoleDto() {
    }
    public RoleDto(Long id, String roleName, String description, Set<PermissionDto> permissions) {
        this.id = id;
        this.roleName = roleName;
        this.description = description;
        this.permissions = permissions;
    }
    public static RoleDtoBuilder builder() {
        return new RoleDtoBuilder();
    }
    public static class RoleDtoBuilder {
        private Long id;
        private String roleName;
        private String description;
        private Set<PermissionDto> permissions;
        RoleDtoBuilder() {}
        public RoleDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public RoleDtoBuilder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }
        public RoleDtoBuilder description(String description) {
            this.description = description;
            return this;
        }
        public RoleDtoBuilder permissions(Set<PermissionDto> permissions) {
            this.permissions = permissions;
            return this;
        }
        public RoleDto build() {
            return new RoleDto(this.id, this.roleName, this.description, this.permissions);
        }
    }
}

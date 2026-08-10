package com.project.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleDto {
    private Long id;

    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "Role code must contain only uppercase letters, numbers, and underscores")
    private String code;

    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private Set<Long> permissionIds;
    private Set<PermissionDto> permissions;

    public RoleDto() {
    }

    public RoleDto(Long id, String code, String name, String description,
                   Set<Long> permissionIds, Set<PermissionDto> permissions) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.permissionIds = permissionIds;
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getRoleName() {
        return name;
    }

    @JsonIgnore
    public void setRoleName(String roleName) {
        this.name = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(Set<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }

    public Set<PermissionDto> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionDto> permissions) {
        this.permissions = permissions;
    }

    public static RoleDtoBuilder builder() {
        return new RoleDtoBuilder();
    }

    public static class RoleDtoBuilder {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Set<Long> permissionIds;
        private Set<PermissionDto> permissions;

        public RoleDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RoleDtoBuilder code(String code) {
            this.code = code;
            return this;
        }

        public RoleDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RoleDtoBuilder roleName(String roleName) {
            this.name = roleName;
            return this;
        }

        public RoleDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RoleDtoBuilder permissionIds(Set<Long> permissionIds) {
            this.permissionIds = permissionIds;
            return this;
        }

        public RoleDtoBuilder permissions(Set<PermissionDto> permissions) {
            this.permissions = permissions;
            return this;
        }

        public RoleDto build() {
            return new RoleDto(id, code, name, description, permissionIds, permissions);
        }
    }
}

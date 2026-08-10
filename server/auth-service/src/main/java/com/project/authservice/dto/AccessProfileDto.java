package com.project.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessProfileDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean active;
    private long assignedAccountCount;

    @NotNull(message = "Danh sách quyền không được để trống")
    private Set<Long> permissionIds;
    private Set<PermissionDto> permissions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public long getAssignedAccountCount() { return assignedAccountCount; }
    public void setAssignedAccountCount(long assignedAccountCount) { this.assignedAccountCount = assignedAccountCount; }
    public Set<Long> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(Set<Long> permissionIds) { this.permissionIds = permissionIds; }
    public Set<PermissionDto> getPermissions() { return permissions; }
    public void setPermissions(Set<PermissionDto> permissions) { this.permissions = permissions; }
}

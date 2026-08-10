package com.project.authservice.dto;

import com.project.authservice.enums.AccountStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class AccountDto {
    private Long id;
    private String email;
    private String roleName;
    private RoleDto role;
    private AccessProfileDto accessProfile;
    private Set<String> assignedCinemaPublicIds = new LinkedHashSet<>();
    private Boolean enabled;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() {
        return this.id;
    }
    public String getEmail() {
        return this.email;
    }
    public String getRoleName() {
        return this.roleName;
    }
    public RoleDto getRole() {
        return role;
    }
    public AccessProfileDto getAccessProfile() { return accessProfile; }
    public Set<String> getAssignedCinemaPublicIds() { return assignedCinemaPublicIds; }
    public Boolean getEnabled() {
        return enabled;
    }
    public AccountStatus getStatus() {
        return this.status;
    }
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    public void setRole(RoleDto role) {
        this.role = role;
    }
    public void setAccessProfile(AccessProfileDto accessProfile) { this.accessProfile = accessProfile; }
    public void setAssignedCinemaPublicIds(Set<String> assignedCinemaPublicIds) {
        this.assignedCinemaPublicIds = assignedCinemaPublicIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(assignedCinemaPublicIds);
    }
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    public void setStatus(AccountStatus status) {
        this.status = status;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public AccountDto() {
    }
    public AccountDto(Long id, String email, String roleName, AccountStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.roleName = roleName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    public static AccountDtoBuilder builder() {
        return new AccountDtoBuilder();
    }
    public static class AccountDtoBuilder {
        private Long id;
        private String email;
        private String roleName;
        private RoleDto role;
        private AccessProfileDto accessProfile;
        private Set<String> assignedCinemaPublicIds;
        private Boolean enabled;
        private AccountStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        AccountDtoBuilder() {}
        public AccountDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public AccountDtoBuilder email(String email) {
            this.email = email;
            return this;
        }
        public AccountDtoBuilder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }
        public AccountDtoBuilder role(RoleDto role) {
            this.role = role;
            return this;
        }
        public AccountDtoBuilder accessProfile(AccessProfileDto accessProfile) {
            this.accessProfile = accessProfile;
            return this;
        }
        public AccountDtoBuilder assignedCinemaPublicIds(Set<String> assignedCinemaPublicIds) {
            this.assignedCinemaPublicIds = assignedCinemaPublicIds;
            return this;
        }
        public AccountDtoBuilder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        public AccountDtoBuilder status(AccountStatus status) {
            this.status = status;
            return this;
        }
        public AccountDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public AccountDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public AccountDto build() {
            AccountDto dto = new AccountDto(this.id, this.email, this.roleName, this.status, this.createdAt, this.updatedAt);
            dto.setRole(this.role);
            dto.setAccessProfile(this.accessProfile);
            dto.setAssignedCinemaPublicIds(this.assignedCinemaPublicIds);
            dto.setEnabled(this.enabled);
            return dto;
        }
    }
}

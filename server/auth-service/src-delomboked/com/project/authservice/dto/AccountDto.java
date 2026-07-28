package com.project.authservice.dto;

import com.project.authservice.enums.AccountStatus;

import java.time.LocalDateTime;

public class AccountDto {
    private Long id;
    private String email;
    private String roleName;
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
            return new AccountDto(this.id, this.email, this.roleName, this.status, this.createdAt, this.updatedAt);
        }
    }
}

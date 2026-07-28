package com.project.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    .Default
    @Column(name = "used", nullable = false)
    private Boolean used = false;
    public Long getId() {
        return this.id;
    }
    public Account getAccount() {
        return this.account;
    }
    public String getToken() {
        return this.token;
    }
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }
    public Boolean isUsed() {
        return this.used;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setAccount(Account account) {
        this.account = account;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    public void setUsed(Boolean used) {
        this.used = used;
    }
    public PasswordResetToken() {
    }
    public PasswordResetToken(Long id, Account account, String token, LocalDateTime createdAt, LocalDateTime expiresAt, Boolean used) {
        this.id = id;
        this.account = account;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = used;
    }
    public static PasswordResetTokenBuilder builder() {
        return new PasswordResetTokenBuilder();
    }
    public static class PasswordResetTokenBuilder {
        private Long id;
        private Account account;
        private String token;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private Boolean used;
        PasswordResetTokenBuilder() {}
        public PasswordResetTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public PasswordResetTokenBuilder account(Account account) {
            this.account = account;
            return this;
        }
        public PasswordResetTokenBuilder token(String token) {
            this.token = token;
            return this;
        }
        public PasswordResetTokenBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public PasswordResetTokenBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        public PasswordResetTokenBuilder used(Boolean used) {
            this.used = used;
            return this;
        }
        public PasswordResetToken build() {
            return new PasswordResetToken(this.id, this.account, this.token, this.createdAt, this.expiresAt, this.used);
        }
    }
}

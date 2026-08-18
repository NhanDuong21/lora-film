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

    @Column(name = "otp_code", nullable = false, length = 6, columnDefinition = "char(6)")
    private String otpCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose = "PASSWORD_RESET";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }

    public Boolean getIsUsed() { return isUsed; }
    public void setIsUsed(Boolean isUsed) { this.isUsed = isUsed; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public PasswordResetToken() {}

    public PasswordResetToken(Long id, Account account, String otpCode, LocalDateTime createdAt, LocalDateTime expiredAt, LocalDateTime usedAt, Integer attempts, Boolean isUsed, String purpose) {
        this.id = id;
        this.account = account;
        this.otpCode = otpCode;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
        this.attempts = attempts;
        this.isUsed = isUsed;
        this.purpose = purpose == null ? "PASSWORD_RESET" : purpose;
    }

    public static PasswordResetTokenBuilder builder() {
        return new PasswordResetTokenBuilder();
    }

    public static class PasswordResetTokenBuilder {
        private Long id;
        private Account account;
        private String otpCode;
        private LocalDateTime createdAt;
        private LocalDateTime expiredAt;
        private LocalDateTime usedAt;
        private Integer attempts;
        private Boolean isUsed;
        private String purpose;

        PasswordResetTokenBuilder() {}

        public PasswordResetTokenBuilder id(Long id) { this.id = id; return this; }
        public PasswordResetTokenBuilder account(Account account) { this.account = account; return this; }
        public PasswordResetTokenBuilder otpCode(String otpCode) { this.otpCode = otpCode; return this; }
        public PasswordResetTokenBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PasswordResetTokenBuilder expiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; return this; }
        public PasswordResetTokenBuilder usedAt(LocalDateTime usedAt) { this.usedAt = usedAt; return this; }
        public PasswordResetTokenBuilder attempts(Integer attempts) { this.attempts = attempts; return this; }
        public PasswordResetTokenBuilder isUsed(Boolean isUsed) { this.isUsed = isUsed; return this; }
        public PasswordResetTokenBuilder purpose(String purpose) { this.purpose = purpose; return this; }

        public PasswordResetToken build() {
            return new PasswordResetToken(id, account, otpCode, createdAt, expiredAt, usedAt, attempts, isUsed, purpose);
        }
    }
}

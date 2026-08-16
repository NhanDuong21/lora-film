package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.AccountingPeriodStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "accounting_periods", uniqueConstraints = @UniqueConstraint(
        name = "uk_accounting_period_scope", columnNames = {"period_code", "scope_key"}))
public class AccountingPeriod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private String publicId;
    @Column(name = "period_code", nullable = false, length = 20)
    private String periodCode;
    @Column(name = "scope_key", nullable = false, length = 50)
    private String scopeKey;
    @Column(name = "cinema_public_id", columnDefinition = "char(36)")
    private String cinemaPublicId;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountingPeriodStatus status = AccountingPeriodStatus.OPEN;
    @Column(name = "created_by_account_id", nullable = false)
    private Long createdByAccountId;
    @Column(name = "reconciled_by_account_id")
    private Long reconciledByAccountId;
    @Column(name = "reconciled_at")
    private Instant reconciledAt;
    @Column(name = "locked_by_account_id")
    private Long lockedByAccountId;
    @Column(name = "locked_at")
    private Instant lockedAt;
    @Column(name = "note_sanitized", length = 1000)
    private String noteSanitized;
    @Version @Column(name = "version", nullable = false)
    private Integer version = 0;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String value) { publicId = value; }
    public String getPeriodCode() { return periodCode; }
    public void setPeriodCode(String value) { periodCode = value; }
    public String getScopeKey() { return scopeKey; }
    public void setScopeKey(String value) { scopeKey = value; }
    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String value) { cinemaPublicId = value; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }
    public AccountingPeriodStatus getStatus() { return status; }
    public void setStatus(AccountingPeriodStatus value) { status = value; }
    public Long getCreatedByAccountId() { return createdByAccountId; }
    public void setCreatedByAccountId(Long value) { createdByAccountId = value; }
    public Long getReconciledByAccountId() { return reconciledByAccountId; }
    public void setReconciledByAccountId(Long value) { reconciledByAccountId = value; }
    public Instant getReconciledAt() { return reconciledAt; }
    public void setReconciledAt(Instant value) { reconciledAt = value; }
    public Long getLockedByAccountId() { return lockedByAccountId; }
    public void setLockedByAccountId(Long value) { lockedByAccountId = value; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant value) { lockedAt = value; }
    public String getNoteSanitized() { return noteSanitized; }
    public void setNoteSanitized(String value) { noteSanitized = value; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

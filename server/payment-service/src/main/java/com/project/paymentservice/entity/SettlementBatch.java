package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.SettlementBatchStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "settlement_batches", uniqueConstraints = @UniqueConstraint(
        name = "uk_settlement_provider_batch", columnNames = {"provider_code", "batch_code"}))
public class SettlementBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private String publicId;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_code", nullable = false, length = 30)
    private ProviderCode providerCode;
    @Column(name = "batch_code", nullable = false, length = 100)
    private String batchCode;
    @Column(name = "cinema_public_id", columnDefinition = "char(36)")
    private String cinemaPublicId;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SettlementBatchStatus status = SettlementBatchStatus.IMPORTED;
    @Column(name = "entry_count", nullable = false)
    private Integer entryCount = 0;
    @Column(name = "matched_count", nullable = false)
    private Integer matchedCount = 0;
    @Column(name = "mismatch_count", nullable = false)
    private Integer mismatchCount = 0;
    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;
    @Column(name = "fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;
    @Column(name = "provider_net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal providerNetAmount = BigDecimal.ZERO;
    @Column(name = "bank_credit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal bankCreditAmount = BigDecimal.ZERO;
    @Column(name = "created_by_account_id", nullable = false)
    private Long createdByAccountId;
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
    public ProviderCode getProviderCode() { return providerCode; }
    public void setProviderCode(ProviderCode value) { providerCode = value; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String value) { batchCode = value; }
    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String value) { cinemaPublicId = value; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String value) { sourceFileName = value; }
    public SettlementBatchStatus getStatus() { return status; }
    public void setStatus(SettlementBatchStatus value) { status = value; }
    public Integer getEntryCount() { return entryCount; }
    public void setEntryCount(Integer value) { entryCount = value; }
    public Integer getMatchedCount() { return matchedCount; }
    public void setMatchedCount(Integer value) { matchedCount = value; }
    public Integer getMismatchCount() { return mismatchCount; }
    public void setMismatchCount(Integer value) { mismatchCount = value; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal value) { grossAmount = value; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal value) { feeAmount = value; }
    public BigDecimal getProviderNetAmount() { return providerNetAmount; }
    public void setProviderNetAmount(BigDecimal value) { providerNetAmount = value; }
    public BigDecimal getBankCreditAmount() { return bankCreditAmount; }
    public void setBankCreditAmount(BigDecimal value) { bankCreditAmount = value; }
    public Long getCreatedByAccountId() { return createdByAccountId; }
    public void setCreatedByAccountId(Long value) { createdByAccountId = value; }
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

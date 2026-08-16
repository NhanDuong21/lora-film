package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.SettlementEntryStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlement_entries", uniqueConstraints = @UniqueConstraint(
        name = "uk_settlement_entry_provider_transaction",
        columnNames = {"settlement_batch_id", "provider_transaction_id"}))
public class SettlementEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_batch_id", nullable = false)
    private SettlementBatch batch;
    @Column(name = "payment_id")
    private Long paymentId;
    @Column(name = "payment_transaction_code", nullable = false, length = 100)
    private String paymentTransactionCode;
    @Column(name = "provider_transaction_id", nullable = false, length = 150)
    private String providerTransactionId;
    @Column(name = "bank_credit_reference", length = 150)
    private String bankCreditReference;
    @Column(name = "provider_gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal providerGrossAmount;
    @Column(name = "provider_fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal providerFeeAmount;
    @Column(name = "provider_net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal providerNetAmount;
    @Column(name = "bank_credit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal bankCreditAmount;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SettlementEntryStatus status;
    @Column(name = "mismatch_reason_sanitized", length = 1000)
    private String mismatchReasonSanitized;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public SettlementBatch getBatch() { return batch; }
    public void setBatch(SettlementBatch value) { batch = value; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long value) { paymentId = value; }
    public String getPaymentTransactionCode() { return paymentTransactionCode; }
    public void setPaymentTransactionCode(String value) { paymentTransactionCode = value; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String value) { providerTransactionId = value; }
    public String getBankCreditReference() { return bankCreditReference; }
    public void setBankCreditReference(String value) { bankCreditReference = value; }
    public BigDecimal getProviderGrossAmount() { return providerGrossAmount; }
    public void setProviderGrossAmount(BigDecimal value) { providerGrossAmount = value; }
    public BigDecimal getProviderFeeAmount() { return providerFeeAmount; }
    public void setProviderFeeAmount(BigDecimal value) { providerFeeAmount = value; }
    public BigDecimal getProviderNetAmount() { return providerNetAmount; }
    public void setProviderNetAmount(BigDecimal value) { providerNetAmount = value; }
    public BigDecimal getBankCreditAmount() { return bankCreditAmount; }
    public void setBankCreditAmount(BigDecimal value) { bankCreditAmount = value; }
    public SettlementEntryStatus getStatus() { return status; }
    public void setStatus(SettlementEntryStatus value) { status = value; }
    public String getMismatchReasonSanitized() { return mismatchReasonSanitized; }
    public void setMismatchReasonSanitized(String value) { mismatchReasonSanitized = value; }
    public Instant getCreatedAt() { return createdAt; }
}

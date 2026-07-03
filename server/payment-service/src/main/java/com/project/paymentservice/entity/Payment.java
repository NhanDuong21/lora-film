package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_transaction_code", unique = true, nullable = false, length = 100)
    private String paymentTransactionCode;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
        private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "provider_order_id", length = 150)
    private String providerOrderId;

    @Column(name = "provider_session_id", length = 150)
    private String providerSessionId;

    @Column(name = "external_transaction_id", length = 150)
    private String externalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
        private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
        private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NONE;

    @Column(name = "reconciliation_reason", length = 255)
    private String reconciliationReason;

    @Column(name = "reconciliation_resolved_at")
    private LocalDateTime reconciliationResolvedAt;

    @Column(name = "settlement_hold_until")
    private LocalDateTime settlementHoldUntil;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message_sanitized", columnDefinition = "TEXT")
    private String failureMessageSanitized;

    @Column(name = "provider_response_code", length = 100)
    private String providerResponseCode;

    @Column(name = "succeeded_at")
    private LocalDateTime succeededAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "latest_provider_summary_sanitized", columnDefinition = "TEXT")
    private String latestProviderSummarySanitized;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Payment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPaymentTransactionCode() {
        return paymentTransactionCode;
    }

    public void setPaymentTransactionCode(String paymentTransactionCode) {
        this.paymentTransactionCode = paymentTransactionCode;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public void setProviderSessionId(String providerSessionId) {
        this.providerSessionId = providerSessionId;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public ReconciliationStatus getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public String getReconciliationReason() {
        return reconciliationReason;
    }

    public void setReconciliationReason(String reconciliationReason) {
        this.reconciliationReason = reconciliationReason;
    }

    public LocalDateTime getReconciliationResolvedAt() {
        return reconciliationResolvedAt;
    }

    public void setReconciliationResolvedAt(LocalDateTime reconciliationResolvedAt) {
        this.reconciliationResolvedAt = reconciliationResolvedAt;
    }

    public LocalDateTime getSettlementHoldUntil() {
        return settlementHoldUntil;
    }

    public void setSettlementHoldUntil(LocalDateTime settlementHoldUntil) {
        this.settlementHoldUntil = settlementHoldUntil;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getFailureMessageSanitized() {
        return failureMessageSanitized;
    }

    public void setFailureMessageSanitized(String failureMessageSanitized) {
        this.failureMessageSanitized = failureMessageSanitized;
    }

    public String getProviderResponseCode() {
        return providerResponseCode;
    }

    public void setProviderResponseCode(String providerResponseCode) {
        this.providerResponseCode = providerResponseCode;
    }

    public LocalDateTime getSucceededAt() {
        return succeededAt;
    }

    public void setSucceededAt(LocalDateTime succeededAt) {
        this.succeededAt = succeededAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getLatestProviderSummarySanitized() {
        return latestProviderSummarySanitized;
    }

    public void setLatestProviderSummarySanitized(String latestProviderSummarySanitized) {
        this.latestProviderSummarySanitized = latestProviderSummarySanitized;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private String publicId;

    @Column(name = "payment_transaction_code", nullable = false, unique = true, length = 100)
    private String paymentTransactionCode;

    @Column(name = "booking_public_id", nullable = false, columnDefinition = "char(36)")
    private String bookingPublicId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency = "VND";

    @Column(name = "booking_amount_locked_at", nullable = false)
    private Instant bookingAmountLockedAt;

    @Column(name = "booking_expires_at", nullable = false)
    private Instant bookingExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_code", nullable = false, length = 30)
    private ProviderCode providerCode;

    @Column(name = "provider_order_id", length = 150)
    private String providerOrderId;

    @Column(name = "provider_session_id", length = 150)
    private String providerSessionId;

    @Column(name = "provider_session_expires_at")
    private Instant providerSessionExpiresAt;

    @Column(name = "external_transaction_id", length = 150)
    private String externalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NONE;

    @Column(name = "reconciliation_reason", length = 500)
    private String reconciliationReason;

    @Column(name = "reconciliation_resolution_code", length = 100)
    private String reconciliationResolutionCode;

    @Column(name = "reconciliation_note_sanitized", columnDefinition = "text")
    private String reconciliationNoteSanitized;

    @Column(name = "reconciliation_resolved_by_account_id")
    private Long reconciliationResolvedByAccountId;

    @Column(name = "reconciliation_resolved_at")
    private Instant reconciliationResolvedAt;

    @Column(name = "settlement_hold_until")
    private Instant settlementHoldUntil;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message_sanitized", columnDefinition = "text")
    private String failureMessageSanitized;

    @Column(name = "provider_response_code", length = 100)
    private String providerResponseCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "latest_provider_summary_sanitized", columnDefinition = "json")
    private String latestProviderSummarySanitized;

    @Column(name = "succeeded_at")
    private Instant succeededAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public Payment() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getPaymentTransactionCode() { return paymentTransactionCode; }
    public void setPaymentTransactionCode(String paymentTransactionCode) { this.paymentTransactionCode = paymentTransactionCode; }
    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String bookingPublicId) { this.bookingPublicId = bookingPublicId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getBookingAmountLockedAt() { return bookingAmountLockedAt; }
    public void setBookingAmountLockedAt(Instant bookingAmountLockedAt) { this.bookingAmountLockedAt = bookingAmountLockedAt; }
    public Instant getBookingExpiresAt() { return bookingExpiresAt; }
    public void setBookingExpiresAt(Instant bookingExpiresAt) { this.bookingExpiresAt = bookingExpiresAt; }
    /** Compatibility alias. */
    public Instant getExpiresAt() { return bookingExpiresAt; }
    /** Compatibility alias. */
    public void setExpiresAt(Instant expiresAt) { this.bookingExpiresAt = expiresAt; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public ProviderCode getProviderCode() { return providerCode; }
    public void setProviderCode(ProviderCode providerCode) { this.providerCode = providerCode; }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }
    public String getProviderSessionId() { return providerSessionId; }
    public void setProviderSessionId(String providerSessionId) { this.providerSessionId = providerSessionId; }
    public Instant getProviderSessionExpiresAt() { return providerSessionExpiresAt; }
    public void setProviderSessionExpiresAt(Instant providerSessionExpiresAt) { this.providerSessionExpiresAt = providerSessionExpiresAt; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    public String getReconciliationReason() { return reconciliationReason; }
    public void setReconciliationReason(String reconciliationReason) { this.reconciliationReason = reconciliationReason; }
    public String getReconciliationResolutionCode() { return reconciliationResolutionCode; }
    public void setReconciliationResolutionCode(String reconciliationResolutionCode) { this.reconciliationResolutionCode = reconciliationResolutionCode; }
    public String getReconciliationNoteSanitized() { return reconciliationNoteSanitized; }
    public void setReconciliationNoteSanitized(String reconciliationNoteSanitized) { this.reconciliationNoteSanitized = reconciliationNoteSanitized; }
    public Long getReconciliationResolvedByAccountId() { return reconciliationResolvedByAccountId; }
    public void setReconciliationResolvedByAccountId(Long value) { this.reconciliationResolvedByAccountId = value; }
    public Instant getReconciliationResolvedAt() { return reconciliationResolvedAt; }
    public void setReconciliationResolvedAt(Instant reconciliationResolvedAt) { this.reconciliationResolvedAt = reconciliationResolvedAt; }
    public Instant getSettlementHoldUntil() { return settlementHoldUntil; }
    public void setSettlementHoldUntil(Instant settlementHoldUntil) { this.settlementHoldUntil = settlementHoldUntil; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getFailureMessageSanitized() { return failureMessageSanitized; }
    public void setFailureMessageSanitized(String failureMessageSanitized) { this.failureMessageSanitized = failureMessageSanitized; }
    public String getProviderResponseCode() { return providerResponseCode; }
    public void setProviderResponseCode(String providerResponseCode) { this.providerResponseCode = providerResponseCode; }
    public String getLatestProviderSummarySanitized() { return latestProviderSummarySanitized; }
    public void setLatestProviderSummarySanitized(String value) { this.latestProviderSummarySanitized = value; }
    public Instant getSucceededAt() { return succeededAt; }
    public void setSucceededAt(Instant succeededAt) { this.succeededAt = succeededAt; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public Instant getExpiredAt() { return expiredAt; }
    public void setExpiredAt(Instant expiredAt) { this.expiredAt = expiredAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

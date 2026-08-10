package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.ActorType;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.RefundComponent;
import com.project.paymentservice.enumtype.RefundStatus;
import com.project.paymentservice.enumtype.RefundType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_refunds")
public class PaymentRefund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private String publicId;
    @Column(name = "refund_code", nullable = false, unique = true, length = 100)
    private String refundCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    @Column(name = "request_key", nullable = false, length = 180)
    private String requestKey;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_code", nullable = false, length = 30)
    private ProviderCode providerCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false, length = 20)
    private RefundType refundType;
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_component", nullable = false, length = 40)
    private RefundComponent refundComponent;
    @Column(name = "reason_code", nullable = false, length = 100)
    private String reasonCode;
    @Column(name = "reason_detail_sanitized", columnDefinition = "text")
    private String reasonDetailSanitized;
    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;
    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency;
    @Column(name = "automatic", nullable = false)
    private boolean automatic;
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by_actor", nullable = false, length = 30)
    private ActorType requestedByActor;
    @Column(name = "requested_by_account_id")
    private Long requestedByAccountId;
    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "review_note_sanitized", columnDefinition = "text")
    private String reviewNoteSanitized;
    @Column(name = "completed_by_account_id")
    private Long completedByAccountId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RefundStatus status = RefundStatus.REQUESTED;
    @Column(name = "provider_order_id", length = 150)
    private String providerOrderId;
    @Column(name = "provider_request_id", length = 150)
    private String providerRequestId;
    @Column(name = "provider_refund_id", length = 150)
    private String providerRefundId;
    @Column(name = "provider_response_code", length = 100)
    private String providerResponseCode;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_summary_sanitized", columnDefinition = "json")
    private String providerSummarySanitized;
    @Column(name = "failure_code", length = 100)
    private String failureCode;
    @Column(name = "failure_message_sanitized", columnDefinition = "text")
    private String failureMessageSanitized;
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "locked_by", length = 100)
    private String lockedBy;
    @Column(name = "locked_at")
    private Instant lockedAt;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "succeeded_at")
    private Instant succeededAt;
    @Column(name = "failed_at")
    private Instant failedAt;
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public PaymentRefund() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String value) { this.publicId = value; }
    public String getRefundCode() { return refundCode; }
    public void setRefundCode(String value) { this.refundCode = value; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment value) { this.payment = value; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { this.requestKey = value; }
    public ProviderCode getProviderCode() { return providerCode; }
    public void setProviderCode(ProviderCode value) { this.providerCode = value; }
    public RefundType getRefundType() { return refundType; }
    public void setRefundType(RefundType value) { this.refundType = value; }
    public RefundComponent getRefundComponent() { return refundComponent; }
    public void setRefundComponent(RefundComponent value) { this.refundComponent = value; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String value) { this.reasonCode = value; }
    public String getReasonDetailSanitized() { return reasonDetailSanitized; }
    public void setReasonDetailSanitized(String value) { this.reasonDetailSanitized = value; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal value) { this.requestedAmount = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
    public boolean isAutomatic() { return automatic; }
    public void setAutomatic(boolean value) { this.automatic = value; }
    public ActorType getRequestedByActor() { return requestedByActor; }
    public void setRequestedByActor(ActorType value) { this.requestedByActor = value; }
    public Long getRequestedByAccountId() { return requestedByAccountId; }
    public void setRequestedByAccountId(Long value) { this.requestedByAccountId = value; }
    public Long getReviewedByAccountId() { return reviewedByAccountId; }
    public void setReviewedByAccountId(Long value) { this.reviewedByAccountId = value; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant value) { this.reviewedAt = value; }
    public String getReviewNoteSanitized() { return reviewNoteSanitized; }
    public void setReviewNoteSanitized(String value) { this.reviewNoteSanitized = value; }
    public Long getCompletedByAccountId() { return completedByAccountId; }
    public void setCompletedByAccountId(Long value) { this.completedByAccountId = value; }
    public RefundStatus getStatus() { return status; }
    public void setStatus(RefundStatus value) { this.status = value; }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String value) { this.providerOrderId = value; }
    public String getProviderRequestId() { return providerRequestId; }
    public void setProviderRequestId(String value) { this.providerRequestId = value; }
    public String getProviderRefundId() { return providerRefundId; }
    public void setProviderRefundId(String value) { this.providerRefundId = value; }
    public String getProviderResponseCode() { return providerResponseCode; }
    public void setProviderResponseCode(String value) { this.providerResponseCode = value; }
    public String getProviderSummarySanitized() { return providerSummarySanitized; }
    public void setProviderSummarySanitized(String value) { this.providerSummarySanitized = value; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String value) { this.failureCode = value; }
    public String getFailureMessageSanitized() { return failureMessageSanitized; }
    public void setFailureMessageSanitized(String value) { this.failureMessageSanitized = value; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int value) { this.retryCount = value; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant value) { this.nextAttemptAt = value; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String value) { this.lockedBy = value; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant value) { this.lockedAt = value; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant value) { this.lockedUntil = value; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant value) { this.requestedAt = value; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant value) { this.submittedAt = value; }
    public Instant getSucceededAt() { return succeededAt; }
    public void setSucceededAt(Instant value) { this.succeededAt = value; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant value) { this.failedAt = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { this.version = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}

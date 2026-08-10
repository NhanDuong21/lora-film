package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.WebhookProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "payment_webhook_events")
public class PaymentWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_code", nullable = false, length = 30)
    private ProviderCode providerCode;
    @Column(name = "provider_event_id", length = 150)
    private String providerEventId;
    @Column(name = "deduplication_key", nullable = false, length = 150)
    private String deduplicationKey;
    @Column(name = "payment_id")
    private Long paymentId;
    @Column(name = "payment_transaction_code", length = 100)
    private String paymentTransactionCode;
    @Column(name = "provider_order_id", length = 150)
    private String providerOrderId;
    @Column(name = "external_transaction_id", length = 150)
    private String externalTransactionId;
    @Column(name = "event_type", length = 50)
    private String eventType;
    @Column(name = "raw_body_hash", nullable = false, columnDefinition = "char(64)")
    private String rawBodyHash;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sanitized_payload", nullable = false, columnDefinition = "json")
    private String sanitizedPayload;
    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid = false;
    @Column(name = "signature_algorithm", length = 50)
    private String signatureAlgorithm;
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.PENDING;
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
    @Column(name = "last_error_sanitized", columnDefinition = "text")
    private String lastErrorSanitized;
    @Column(name = "locked_by", length = 100)
    private String lockedBy;
    @Column(name = "locked_at")
    private Instant lockedAt;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "received_at", insertable = false, updatable = false)
    private Instant receivedAt;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public PaymentWebhookEvent() {
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProviderCode getProviderCode() { return providerCode; }
    public void setProviderCode(ProviderCode providerCode) { this.providerCode = providerCode; }
    public String getProvider() { return providerCode == null ? null : providerCode.name(); }
    public void setProvider(String provider) { this.providerCode = provider == null ? null : ProviderCode.valueOf(provider); }
    public String getProviderEventId() { return providerEventId; }
    public void setProviderEventId(String value) { this.providerEventId = value; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public void setDeduplicationKey(String value) { this.deduplicationKey = value; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public String getPaymentTransactionCode() { return paymentTransactionCode; }
    public void setPaymentTransactionCode(String value) { this.paymentTransactionCode = value; }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String value) { this.providerOrderId = value; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String value) { this.externalTransactionId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getRawBodyHash() { return rawBodyHash; }
    public void setRawBodyHash(String value) { this.rawBodyHash = value; }
    public String getPayloadHash() { return rawBodyHash; }
    public void setPayloadHash(String value) { this.rawBodyHash = value; }
    public String getSanitizedPayload() { return sanitizedPayload; }
    public void setSanitizedPayload(String value) { this.sanitizedPayload = value; }
    public Boolean getSignatureValid() { return signatureValid; }
    public void setSignatureValid(Boolean value) { this.signatureValid = value; }
    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public void setSignatureAlgorithm(String value) { this.signatureAlgorithm = value; }
    public WebhookProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(WebhookProcessingStatus value) { this.processingStatus = value; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer value) { this.retryCount = value; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant value) { this.nextRetryAt = value; }
    public String getLastErrorSanitized() { return lastErrorSanitized; }
    public void setLastErrorSanitized(String value) { this.lastErrorSanitized = value; }
    public String getLastError() { return lastErrorSanitized; }
    public void setLastError(String value) { this.lastErrorSanitized = value; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String value) { this.lockedBy = value; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant value) { this.lockedAt = value; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant value) { this.lockedUntil = value; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant value) { this.receivedAt = value; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant value) { this.processedAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}

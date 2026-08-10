package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.IdempotencyProcessingStatus;
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
@Table(name = "payment_idempotency_records")
public class PaymentIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Column(name = "operation", nullable = false, length = 50)
    private String operation;
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, columnDefinition = "char(64)")
    private String requestHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private IdempotencyProcessingStatus processingStatus = IdempotencyProcessingStatus.PROCESSING;
    @Column(name = "payment_id")
    private Long paymentId;
    @Column(name = "response_status")
    private Integer responseStatus;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body_sanitized", columnDefinition = "json")
    private String responseBodySanitized;
    @Column(name = "error_code", length = 100)
    private String errorCode;
    @Column(name = "last_error_sanitized", columnDefinition = "text")
    private String lastErrorSanitized;
    @Column(name = "locked_by", length = 100)
    private String lockedBy;
    @Column(name = "locked_at")
    private Instant lockedAt;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public PaymentIdempotencyRecord() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public IdempotencyProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(IdempotencyProcessingStatus status) { this.processingStatus = status; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseBodySanitized() { return responseBodySanitized; }
    public void setResponseBodySanitized(String value) { this.responseBodySanitized = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getLastErrorSanitized() { return lastErrorSanitized; }
    public void setLastErrorSanitized(String value) { this.lastErrorSanitized = value; }
    /** Compatibility alias. */
    public String getLastError() { return lastErrorSanitized; }
    /** Compatibility alias. */
    public void setLastError(String value) { this.lastErrorSanitized = value; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

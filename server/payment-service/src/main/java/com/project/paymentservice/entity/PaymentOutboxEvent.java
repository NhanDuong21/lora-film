package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.OutboxDestination;
import com.project.paymentservice.enumtype.OutboxStatus;
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
@Table(name = "payment_outbox_events")
public class PaymentOutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private String eventId;
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion;
    @Enumerated(EnumType.STRING)
    @Column(name = "destination", nullable = false, length = 100)
    private OutboxDestination destination;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxStatus status = OutboxStatus.PENDING;
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
    @Column(name = "last_error_sanitized", columnDefinition = "text")
    private String lastErrorSanitized;
    @Column(name = "correlation_id", length = 100)
    private String correlationId;
    @Column(name = "trace_id", length = 100)
    private String traceId;
    @Column(name = "locked_by", length = 100)
    private String lockedBy;
    @Column(name = "locked_at")
    private Instant lockedAt;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
    @Column(name = "published_at")
    private Instant publishedAt;

    public PaymentOutboxEvent() {
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public OutboxDestination getDestination() { return destination; }
    public void setDestination(OutboxDestination destination) { this.destination = destination; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getLastErrorSanitized() { return lastErrorSanitized; }
    public void setLastErrorSanitized(String value) { this.lastErrorSanitized = value; }
    public String getLastError() { return lastErrorSanitized; }
    public void setLastError(String value) { this.lastErrorSanitized = value; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}

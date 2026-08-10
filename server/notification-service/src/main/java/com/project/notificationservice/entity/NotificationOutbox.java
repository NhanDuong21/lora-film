package com.project.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;
    @Column(name = "aggregate_public_id", nullable = false, length = 80)
    private String aggregatePublicId;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;

    public NotificationOutbox() {
    }

    @PrePersist
    public void beforeInsert() {
        if (eventId == null) eventId = UUID.randomUUID().toString();
        if (status == null) status = "PENDING";
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getAggregatePublicId() { return aggregatePublicId; }
    public void setAggregatePublicId(String value) { this.aggregatePublicId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { this.eventType = value; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String value) { this.payloadJson = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int value) { this.attemptCount = value; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant value) { this.nextRetryAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant value) { this.publishedAt = value; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof NotificationOutbox other
                && eventId != null && eventId.equals(other.eventId);
    }

    @Override
    public int hashCode() { return Objects.hashCode(eventId); }

    @Override
    public String toString() {
        return "NotificationOutbox{eventId='" + eventId + "', eventType='" + eventType
                + "', status='" + status + "'}";
    }
}

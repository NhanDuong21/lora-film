package com.project.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "notification_dead_letters")
public class NotificationDeadLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "notification_delivery_id", nullable = false, unique = true)
    private Long notificationDeliveryId;
    @Column(nullable = false, length = 80)
    private String reason;
    @Column(name = "failure_message", length = 500)
    private String failureMessage;
    @Column(name = "reprocess_count", nullable = false)
    private int reprocessCount;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "reprocessed_at")
    private Instant reprocessedAt;

    public NotificationDeadLetter() {
    }

    @PrePersist
    public void beforeInsert() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getNotificationDeliveryId() { return notificationDeliveryId; }
    public void setNotificationDeliveryId(Long value) { this.notificationDeliveryId = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String value) { this.failureMessage = value; }
    public int getReprocessCount() { return reprocessCount; }
    public void setReprocessCount(int value) { this.reprocessCount = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReprocessedAt() { return reprocessedAt; }
    public void setReprocessedAt(Instant value) { this.reprocessedAt = value; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof NotificationDeadLetter other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "NotificationDeadLetter{id=" + id + ", notificationDeliveryId="
                + notificationDeliveryId + ", reason='" + reason + "'}";
    }
}

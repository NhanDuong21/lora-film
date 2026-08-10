package com.project.notificationservice.entity;

import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "notification_delivery_attempts")
public class NotificationDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "notification_delivery_id", nullable = false)
    private Long notificationDeliveryId;
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;
    @Column(nullable = false, length = 50)
    private String provider;
    @Column(nullable = false, length = 30)
    private String outcome;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "failure_category", length = 40)
    private FailureCategory failureCategory;
    @Column(name = "failure_code", length = 80)
    private String failureCode;
    @Column(name = "failure_message", length = 500)
    private String failureMessage;
    @Column(name = "duration_ms", nullable = false)
    private long durationMs;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public NotificationDeliveryAttempt() {
    }

    @PrePersist
    public void beforeInsert() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getNotificationDeliveryId() { return notificationDeliveryId; }
    public void setNotificationDeliveryId(Long value) { this.notificationDeliveryId = value; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int value) { this.attemptNumber = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { this.provider = value; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String value) { this.outcome = value; }
    public FailureCategory getFailureCategory() { return failureCategory; }
    public void setFailureCategory(FailureCategory value) { this.failureCategory = value; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String value) { this.failureCode = value; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String value) { this.failureMessage = value; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long value) { this.durationMs = value; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof NotificationDeliveryAttempt other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "NotificationDeliveryAttempt{id=" + id + ", attemptNumber=" + attemptNumber
                + ", outcome='" + outcome + "'}";
    }
}

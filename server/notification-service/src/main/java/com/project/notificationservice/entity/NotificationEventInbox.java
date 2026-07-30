package com.project.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "notification_event_inbox", uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_inbox_source_event",
        columnNames = {"source_service", "source_event_id"}))
public class NotificationEventInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "source_service", nullable = false, length = 80)
    private String sourceService;
    @Column(name = "source_event_id", nullable = false, length = 80)
    private String sourceEventId;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(name = "event_version", nullable = false)
    private int eventVersion;
    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    public NotificationEventInbox() {
    }

    @PrePersist
    public void beforeInsert() {
        receivedAt = Instant.now();
        if (status == null) status = "RECEIVED";
    }

    public Long getId() { return id; }
    public String getSourceService() { return sourceService; }
    public void setSourceService(String value) { this.sourceService = value; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String value) { this.sourceEventId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { this.eventType = value; }
    public int getEventVersion() { return eventVersion; }
    public void setEventVersion(int value) { this.eventVersion = value; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String value) { this.payloadJson = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant value) { this.processedAt = value; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String value) { this.failureMessage = value; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof NotificationEventInbox other)) return false;
        return Objects.equals(sourceService, other.sourceService)
                && Objects.equals(sourceEventId, other.sourceEventId);
    }

    @Override
    public int hashCode() { return Objects.hash(sourceService, sourceEventId); }

    @Override
    public String toString() {
        return "NotificationEventInbox{sourceService='" + sourceService
                + "', sourceEventId='" + sourceEventId + "', status='" + status + "'}";
    }
}

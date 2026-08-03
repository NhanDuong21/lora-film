package com.project.notificationservice.entity;

import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Priority;
import com.project.notificationservice.domain.NotificationTypes.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notification_requests")
public class NotificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;
    @Column(name = "source_service", nullable = false, length = 80)
    private String sourceService;
    @Column(name = "source_event_id", length = 80)
    private String sourceEventId;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(name = "correlation_id", length = 80)
    private String correlationId;
    @Column(name = "causation_id", length = 80)
    private String causationId;
    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;
    @Column(name = "template_commit_sha", length = 64)
    private String templateCommitSha;
    @Column(name = "template_version", length = 40)
    private String templateVersion;
    @Column(nullable = false, length = 20)
    private String locale;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private Category category;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Priority priority;
    @Column(name = "scheduled_at")
    private Instant scheduledAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private RequestStatus status;
    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;
    @Column(name = "is_test", nullable = false)
    private boolean test;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public NotificationRequest() {
    }

    @PrePersist
    public void beforeInsert() {
        Instant now = Instant.now();
        if (publicId == null) publicId = UUID.randomUUID().toString();
        if (status == null) status = RequestStatus.ACCEPTED;
        if (priority == null) priority = Priority.NORMAL;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public String getSourceService() { return sourceService; }
    public void setSourceService(String value) { this.sourceService = value; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String value) { this.sourceEventId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { this.eventType = value; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String value) { this.correlationId = value; }
    public String getCausationId() { return causationId; }
    public void setCausationId(String value) { this.causationId = value; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String value) { this.templateKey = value; }
    public String getTemplateCommitSha() { return templateCommitSha; }
    public void setTemplateCommitSha(String value) { this.templateCommitSha = value; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String value) { this.templateVersion = value; }
    public String getLocale() { return locale; }
    public void setLocale(String value) { this.locale = value; }
    public Category getCategory() { return category; }
    public void setCategory(Category value) { this.category = value; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority value) { this.priority = value; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant value) { this.scheduledAt = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus value) { this.status = value; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String value) { this.payloadJson = value; }
    public boolean isTest() { return test; }
    public void setTest(boolean value) { this.test = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof NotificationRequest other
                && publicId != null && publicId.equals(other.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }

    @Override
    public String toString() {
        return "NotificationRequest{publicId='" + publicId + "', eventType='" + eventType
                + "', templateKey='" + templateKey + "', status=" + status + "}";
    }
}

package com.project.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "notification_audit_logs")
public class NotificationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_public_id", nullable = false, length = 80)
    private String actorPublicId;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;
    @Column(name = "target_public_id", nullable = false, length = 150)
    private String targetPublicId;
    @Lob
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public NotificationAuditLog() {
    }

    @PrePersist
    public void beforeInsert() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getActorPublicId() { return actorPublicId; }
    public void setActorPublicId(String value) { this.actorPublicId = value; }
    public String getAction() { return action; }
    public void setAction(String value) { this.action = value; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String value) { this.targetType = value; }
    public String getTargetPublicId() { return targetPublicId; }
    public void setTargetPublicId(String value) { this.targetPublicId = value; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String value) { this.metadataJson = value; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof NotificationAuditLog other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "NotificationAuditLog{id=" + id + ", actorPublicId='" + actorPublicId
                + "', action='" + action + "', targetPublicId='" + targetPublicId + "'}";
    }
}

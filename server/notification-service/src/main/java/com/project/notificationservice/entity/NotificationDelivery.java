package com.project.notificationservice.entity;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.DeliveryStatus;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
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
@Table(name = "notification_deliveries")
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "notification_request_id", nullable = false)
    private Long notificationRequestId;
    @Column(name = "notification_recipient_id", nullable = false)
    private Long notificationRecipientId;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Channel channel;
    @Column(nullable = false, length = 50)
    private String provider;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status;
    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;
    @Column(name = "template_commit_sha", length = 64)
    private String templateCommitSha;
    @Column(name = "template_version", length = 40)
    private String templateVersion;
    @Column(name = "rendered_subject", length = 200)
    private String renderedSubject;
    @Lob
    @Column(name = "rendered_html", columnDefinition = "LONGTEXT")
    private String renderedHtml;
    @Lob
    @Column(name = "rendered_text", columnDefinition = "LONGTEXT")
    private String renderedText;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "failure_category", length = 40)
    private FailureCategory failureCategory;
    @Column(name = "failure_code", length = 80)
    private String failureCode;
    @Column(name = "failure_message", length = 500)
    private String failureMessage;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "failed_at")
    private Instant failedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public NotificationDelivery() {
    }

    @PrePersist
    public void beforeInsert() {
        Instant now = Instant.now();
        if (publicId == null) publicId = UUID.randomUUID().toString();
        if (status == null) status = DeliveryStatus.PENDING;
        if (provider == null) provider = "internal";
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public Long getNotificationRequestId() { return notificationRequestId; }
    public void setNotificationRequestId(Long value) { this.notificationRequestId = value; }
    public Long getNotificationRecipientId() { return notificationRecipientId; }
    public void setNotificationRecipientId(Long value) { this.notificationRecipientId = value; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel value) { this.channel = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { this.provider = value; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus value) { this.status = value; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String value) { this.providerMessageId = value; }
    public String getTemplateCommitSha() { return templateCommitSha; }
    public void setTemplateCommitSha(String value) { this.templateCommitSha = value; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String value) { this.templateVersion = value; }
    public String getRenderedSubject() { return renderedSubject; }
    public void setRenderedSubject(String value) { this.renderedSubject = value; }
    public String getRenderedHtml() { return renderedHtml; }
    public void setRenderedHtml(String value) { this.renderedHtml = value; }
    public String getRenderedText() { return renderedText; }
    public void setRenderedText(String value) { this.renderedText = value; }
    public FailureCategory getFailureCategory() { return failureCategory; }
    public void setFailureCategory(FailureCategory value) { this.failureCategory = value; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String value) { this.failureCode = value; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String value) { this.failureMessage = value; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int value) { this.attemptCount = value; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant value) { this.nextRetryAt = value; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant value) { this.sentAt = value; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant value) { this.deliveredAt = value; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant value) { this.failedAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof NotificationDelivery other
                && publicId != null && publicId.equals(other.publicId);
    }

    @Override
    public int hashCode() { return Objects.hashCode(publicId); }

    @Override
    public String toString() {
        return "NotificationDelivery{publicId='" + publicId + "', channel=" + channel
                + ", status=" + status + ", attemptCount=" + attemptCount + "}";
    }
}

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
@Table(name = "notification_recipients")
public class NotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "notification_request_id", nullable = false)
    private Long notificationRequestId;
    @Column(name = "user_public_id", length = 80)
    private String userPublicId;
    @Column(name = "recipient_type", nullable = false, length = 30)
    private String recipientType;
    @Column(name = "email_encrypted", length = 1000)
    private String emailEncrypted;
    @Column(name = "phone_encrypted", length = 1000)
    private String phoneEncrypted;
    @Column(name = "web_push_subscription_encrypted", length = 4000)
    private String webPushSubscriptionEncrypted;
    @Column(nullable = false, length = 20)
    private String locale;
    @Column(nullable = false, length = 60)
    private String timezone;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public NotificationRecipient() {
    }

    @PrePersist
    public void beforeInsert() {
        createdAt = Instant.now();
        if (recipientType == null) recipientType = "CUSTOMER";
        if (timezone == null) timezone = "Asia/Ho_Chi_Minh";
    }

    public Long getId() { return id; }
    public Long getNotificationRequestId() { return notificationRequestId; }
    public void setNotificationRequestId(Long value) { this.notificationRequestId = value; }
    public String getUserPublicId() { return userPublicId; }
    public void setUserPublicId(String value) { this.userPublicId = value; }
    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String value) { this.recipientType = value; }
    public String getEmailEncrypted() { return emailEncrypted; }
    public void setEmailEncrypted(String value) { this.emailEncrypted = value; }
    public String getPhoneEncrypted() { return phoneEncrypted; }
    public void setPhoneEncrypted(String value) { this.phoneEncrypted = value; }
    public String getWebPushSubscriptionEncrypted() { return webPushSubscriptionEncrypted; }
    public void setWebPushSubscriptionEncrypted(String value) { this.webPushSubscriptionEncrypted = value; }
    public String getLocale() { return locale; }
    public void setLocale(String value) { this.locale = value; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String value) { this.timezone = value; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof NotificationRecipient other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "NotificationRecipient{id=" + id + ", userPublicId='" + userPublicId + "'}";
    }
}

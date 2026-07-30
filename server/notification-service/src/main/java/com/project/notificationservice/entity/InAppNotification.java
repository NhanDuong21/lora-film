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
import java.util.UUID;

@Entity
@Table(name = "in_app_notifications")
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "notification_delivery_id", nullable = false, unique = true)
    private Long notificationDeliveryId;
    @Column(name = "user_public_id", nullable = false, length = 80)
    private String userPublicId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 2000)
    private String body;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(name = "deep_link", length = 500)
    private String deepLink;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InAppNotification() {
    }

    @PrePersist
    public void beforeInsert() {
        if (publicId == null) publicId = UUID.randomUUID().toString();
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public Long getNotificationDeliveryId() { return notificationDeliveryId; }
    public void setNotificationDeliveryId(Long value) { this.notificationDeliveryId = value; }
    public String getUserPublicId() { return userPublicId; }
    public void setUserPublicId(String value) { this.userPublicId = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { this.title = value; }
    public String getBody() { return body; }
    public void setBody(String value) { this.body = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { this.category = value; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String value) { this.deepLink = value; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant value) { this.readAt = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof InAppNotification other
                && publicId != null && publicId.equals(other.publicId);
    }

    @Override
    public int hashCode() { return Objects.hashCode(publicId); }

    @Override
    public String toString() {
        return "InAppNotification{publicId='" + publicId + "', userPublicId='"
                + userPublicId + "', readAt=" + readAt + "}";
    }
}

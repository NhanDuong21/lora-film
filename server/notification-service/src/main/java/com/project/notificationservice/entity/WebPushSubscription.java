package com.project.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "web_push_subscriptions")
public class WebPushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "user_public_id", nullable = false, length = 80)
    private String userPublicId;
    @Column(name = "endpoint_encrypted", nullable = false, length = 3000)
    private String endpointEncrypted;
    @Column(name = "p256dh_encrypted", nullable = false, length = 1000)
    private String p256dhEncrypted;
    @Column(name = "auth_encrypted", nullable = false, length = 1000)
    private String authEncrypted;
    @Column(name = "user_agent_hash", length = 64)
    private String userAgentHash;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WebPushSubscription() {
    }

    @PrePersist
    public void beforeInsert() {
        Instant now = Instant.now();
        if (publicId == null) publicId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String value) { this.publicId = value; }
    public String getUserPublicId() { return userPublicId; }
    public void setUserPublicId(String value) { this.userPublicId = value; }
    public String getEndpointEncrypted() { return endpointEncrypted; }
    public void setEndpointEncrypted(String value) { this.endpointEncrypted = value; }
    public String getP256dhEncrypted() { return p256dhEncrypted; }
    public void setP256dhEncrypted(String value) { this.p256dhEncrypted = value; }
    public String getAuthEncrypted() { return authEncrypted; }
    public void setAuthEncrypted(String value) { this.authEncrypted = value; }
    public String getUserAgentHash() { return userAgentHash; }
    public void setUserAgentHash(String value) { this.userAgentHash = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { this.active = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof WebPushSubscription other
                && publicId != null && publicId.equals(other.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }

    @Override
    public String toString() {
        return "WebPushSubscription{publicId='" + publicId + "', userPublicId='"
                + userPublicId + "', active=" + active + "}";
    }
}

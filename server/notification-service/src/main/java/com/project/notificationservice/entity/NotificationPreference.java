package com.project.notificationservice.entity;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_preference_user_channel_category",
        columnNames = {"user_public_id", "channel", "category"}))
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_public_id", nullable = false, length = 80)
    private String userPublicId;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Channel channel;
    @Column(nullable = false, length = 30)
    private String category;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationPreference() {
    }

    @PrePersist
    @PreUpdate
    public void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getUserPublicId() { return userPublicId; }
    public void setUserPublicId(String value) { this.userPublicId = value; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel value) { this.channel = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { this.category = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { this.enabled = value; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof NotificationPreference other)) return false;
        return Objects.equals(userPublicId, other.userPublicId)
                && channel == other.channel && Objects.equals(category, other.category);
    }

    @Override
    public int hashCode() { return Objects.hash(userPublicId, channel, category); }

    @Override
    public String toString() {
        return "NotificationPreference{userPublicId='" + userPublicId
                + "', channel=" + channel + ", category='" + category + "', enabled=" + enabled + "}";
    }
}

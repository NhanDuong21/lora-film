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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "notification_suppressions", uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_suppression_destination",
        columnNames = {"destination_hash", "channel"}))
public class NotificationSuppression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "destination_hash", nullable = false, length = 64)
    private String destinationHash;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Channel channel;
    @Column(nullable = false, length = 80)
    private String reason;
    @Column(nullable = false, length = 80)
    private String source;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public NotificationSuppression() {
    }

    @PrePersist
    public void beforeInsert() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getDestinationHash() { return destinationHash; }
    public void setDestinationHash(String value) { this.destinationHash = value; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel value) { this.channel = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }
    public String getSource() { return source; }
    public void setSource(String value) { this.source = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof NotificationSuppression other
                && Objects.equals(destinationHash, other.destinationHash)
                && channel == other.channel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationHash, channel);
    }

    @Override
    public String toString() {
        return "NotificationSuppression{destinationHash='" + destinationHash
                + "', channel=" + channel + ", reason='" + reason + "'}";
    }
}

package com.lorafilm.movie.showtime.domain.entity;

import com.lorafilm.movie.showtime.domain.enums.ShowtimeRefundOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "showtime_refund_outbox")
public class ShowtimeRefundOutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private String eventId;
    @Column(name = "showtime_public_id", nullable = false, columnDefinition = "char(36)")
    private String showtimePublicId;
    @Column(name = "cancellation_reason", nullable = false, length = 1000)
    private String cancellationReason;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShowtimeRefundOutboxStatus status = ShowtimeRefundOutboxStatus.PENDING;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "locked_by", length = 100)
    private String lockedBy;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "last_error", length = 2000)
    private String lastError;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String value) { this.eventId = value; }
    public String getShowtimePublicId() { return showtimePublicId; }
    public void setShowtimePublicId(String value) { this.showtimePublicId = value; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String value) { this.cancellationReason = value; }
    public ShowtimeRefundOutboxStatus getStatus() { return status; }
    public void setStatus(ShowtimeRefundOutboxStatus value) { this.status = value; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int value) { this.attemptCount = value; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant value) { this.nextAttemptAt = value; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String value) { this.lockedBy = value; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant value) { this.lockedUntil = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { this.lastError = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant value) { this.publishedAt = value; }
}

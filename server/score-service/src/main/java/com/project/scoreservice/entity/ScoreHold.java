package com.project.scoreservice.entity;

import com.project.scoreservice.enumtype.ScoreHoldStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "score_holds", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hold_code", columnNames = "hold_code"),
        @UniqueConstraint(name = "uk_hold_booking", columnNames = "booking_id"),
        @UniqueConstraint(name = "uk_hold_idempotency", columnNames = "idempotency_key")
})
public class ScoreHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hold_code", nullable = false, unique = true, length = 80)
    private String holdCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserScore userScore;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScoreHoldStatus status = ScoreHoldStatus.ACTIVE;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "committed_at")
    private LocalDateTime committedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "event_id", length = 150)
    private String eventId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "source_service", nullable = false, length = 50)
    private String sourceService = "BOOKING";

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ScoreHold() {
    }

    public ScoreHold(Long id, String holdCode, UserScore userScore, Long bookingId, Integer points, ScoreHoldStatus status, LocalDateTime expiredAt, LocalDateTime committedAt, LocalDateTime releasedAt, String eventId, String idempotencyKey, String requestId, String sourceService, String correlationId, String metadata, LocalDateTime createdAt) {
        this.id = id;
        this.holdCode = holdCode;
        this.userScore = userScore;
        this.bookingId = bookingId;
        this.points = points;
        this.status = status != null ? status : ScoreHoldStatus.ACTIVE;
        this.expiredAt = expiredAt;
        this.committedAt = committedAt;
        this.releasedAt = releasedAt;
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.requestId = requestId;
        this.sourceService = sourceService != null ? sourceService : "BOOKING";
        this.correlationId = correlationId;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHoldCode() { return holdCode; }
    public void setHoldCode(String holdCode) { this.holdCode = holdCode; }

    public UserScore getUserScore() { return userScore; }
    public void setUserScore(UserScore userScore) { this.userScore = userScore; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public ScoreHoldStatus getStatus() { return status; }
    public void setStatus(ScoreHoldStatus status) { this.status = status; }

    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }

    public LocalDateTime getCommittedAt() { return committedAt; }
    public void setCommittedAt(LocalDateTime committedAt) { this.committedAt = committedAt; }

    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static ScoreHoldBuilder builder() { return new ScoreHoldBuilder(); }

    public static class ScoreHoldBuilder {
        private Long id;
        private String holdCode;
        private UserScore userScore;
        private Long bookingId;
        private Integer points;
        private ScoreHoldStatus status;
        private LocalDateTime expiredAt;
        private LocalDateTime committedAt;
        private LocalDateTime releasedAt;
        private String eventId;
        private String idempotencyKey;
        private String requestId;
        private String sourceService;
        private String correlationId;
        private String metadata;
        private LocalDateTime createdAt;

        public ScoreHoldBuilder id(Long id) { this.id = id; return this; }
        public ScoreHoldBuilder holdCode(String holdCode) { this.holdCode = holdCode; return this; }
        public ScoreHoldBuilder userScore(UserScore userScore) { this.userScore = userScore; return this; }
        public ScoreHoldBuilder bookingId(Long bookingId) { this.bookingId = bookingId; return this; }
        public ScoreHoldBuilder points(Integer points) { this.points = points; return this; }
        public ScoreHoldBuilder status(ScoreHoldStatus status) { this.status = status; return this; }
        public ScoreHoldBuilder expiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; return this; }
        public ScoreHoldBuilder committedAt(LocalDateTime committedAt) { this.committedAt = committedAt; return this; }
        public ScoreHoldBuilder releasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; return this; }
        public ScoreHoldBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public ScoreHoldBuilder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public ScoreHoldBuilder requestId(String requestId) { this.requestId = requestId; return this; }
        public ScoreHoldBuilder sourceService(String sourceService) { this.sourceService = sourceService; return this; }
        public ScoreHoldBuilder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public ScoreHoldBuilder metadata(String metadata) { this.metadata = metadata; return this; }
        public ScoreHoldBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ScoreHold build() {
            return new ScoreHold(id, holdCode, userScore, bookingId, points, status, expiredAt, committedAt, releasedAt, eventId, idempotencyKey, requestId, sourceService, correlationId, metadata, createdAt);
        }
    }
}

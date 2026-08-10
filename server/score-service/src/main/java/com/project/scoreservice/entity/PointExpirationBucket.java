package com.project.scoreservice.entity;

import com.project.scoreservice.enumtype.PointExpirationBucketStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_expiration_buckets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bucket_history", columnNames = "history_id")
})
public class PointExpirationBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserScore userScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private ScoreHistory scoreHistory;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "earned_points", nullable = false)
    private Integer earnedPoints;

    @Column(name = "remaining_points", nullable = false)
    private Integer remainingPoints;

    @Column(name = "expired_points", nullable = false)
    private Integer expiredPoints = 0;

    @Column(name = "consumed_points", nullable = false)
    private Integer consumedPoints = 0;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PointExpirationBucketStatus status = PointExpirationBucketStatus.ACTIVE;

    @Column(name = "tier_snapshot", length = 30)
    private String tierSnapshot;

    @Column(name = "event_id", length = 150)
    private String eventId;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PointExpirationBucket() {}

    public PointExpirationBucket(Long id, UserScore userScore, ScoreHistory scoreHistory, Long bookingId,
                                 Integer earnedPoints, Integer remainingPoints, Integer expiredPoints, Integer consumedPoints,
                                 LocalDate expirationDate, PointExpirationBucketStatus status, String tierSnapshot,
                                 String eventId, String metadata, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userScore = userScore;
        this.scoreHistory = scoreHistory;
        this.bookingId = bookingId;
        this.earnedPoints = earnedPoints;
        this.remainingPoints = remainingPoints;
        this.expiredPoints = expiredPoints != null ? expiredPoints : 0;
        this.consumedPoints = consumedPoints != null ? consumedPoints : 0;
        this.expirationDate = expirationDate;
        this.status = status != null ? status : PointExpirationBucketStatus.ACTIVE;
        this.tierSnapshot = tierSnapshot;
        this.eventId = eventId;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PointExpirationBucketBuilder builder() {
        return new PointExpirationBucketBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserScore getUserScore() { return userScore; }
    public void setUserScore(UserScore userScore) { this.userScore = userScore; }

    public ScoreHistory getScoreHistory() { return scoreHistory; }
    public void setScoreHistory(ScoreHistory scoreHistory) { this.scoreHistory = scoreHistory; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Integer getEarnedPoints() { return earnedPoints; }
    public void setEarnedPoints(Integer earnedPoints) { this.earnedPoints = earnedPoints; }

    public Integer getRemainingPoints() { return remainingPoints; }
    public void setRemainingPoints(Integer remainingPoints) { this.remainingPoints = remainingPoints; }

    public Integer getExpiredPoints() { return expiredPoints; }
    public void setExpiredPoints(Integer expiredPoints) { this.expiredPoints = expiredPoints; }

    public Integer getConsumedPoints() { return consumedPoints; }
    public void setConsumedPoints(Integer consumedPoints) { this.consumedPoints = consumedPoints; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public PointExpirationBucketStatus getStatus() { return status; }
    public void setStatus(PointExpirationBucketStatus status) { this.status = status; }

    public String getTierSnapshot() { return tierSnapshot; }
    public void setTierSnapshot(String tierSnapshot) { this.tierSnapshot = tierSnapshot; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class PointExpirationBucketBuilder {
        private Long id;
        private UserScore userScore;
        private ScoreHistory scoreHistory;
        private Long bookingId;
        private Integer earnedPoints;
        private Integer remainingPoints;
        private Integer expiredPoints = 0;
        private Integer consumedPoints = 0;
        private LocalDate expirationDate;
        private PointExpirationBucketStatus status = PointExpirationBucketStatus.ACTIVE;
        private String tierSnapshot;
        private String eventId;
        private String metadata;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public PointExpirationBucketBuilder id(Long id) { this.id = id; return this; }
        public PointExpirationBucketBuilder userScore(UserScore userScore) { this.userScore = userScore; return this; }
        public PointExpirationBucketBuilder scoreHistory(ScoreHistory scoreHistory) { this.scoreHistory = scoreHistory; return this; }
        public PointExpirationBucketBuilder bookingId(Long bookingId) { this.bookingId = bookingId; return this; }
        public PointExpirationBucketBuilder earnedPoints(Integer earnedPoints) { this.earnedPoints = earnedPoints; return this; }
        public PointExpirationBucketBuilder remainingPoints(Integer remainingPoints) { this.remainingPoints = remainingPoints; return this; }
        public PointExpirationBucketBuilder expiredPoints(Integer expiredPoints) { this.expiredPoints = expiredPoints; return this; }
        public PointExpirationBucketBuilder consumedPoints(Integer consumedPoints) { this.consumedPoints = consumedPoints; return this; }
        public PointExpirationBucketBuilder expirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; return this; }
        public PointExpirationBucketBuilder status(PointExpirationBucketStatus status) { this.status = status; return this; }
        public PointExpirationBucketBuilder tierSnapshot(String tierSnapshot) { this.tierSnapshot = tierSnapshot; return this; }
        public PointExpirationBucketBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public PointExpirationBucketBuilder metadata(String metadata) { this.metadata = metadata; return this; }
        public PointExpirationBucketBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PointExpirationBucketBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public PointExpirationBucket build() {
            return new PointExpirationBucket(id, userScore, scoreHistory, bookingId, earnedPoints, remainingPoints,
                    expiredPoints, consumedPoints, expirationDate, status, tierSnapshot, eventId, metadata, createdAt, updatedAt);
        }
    }
}

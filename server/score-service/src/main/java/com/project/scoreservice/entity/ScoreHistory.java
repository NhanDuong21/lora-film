package com.project.scoreservice.entity;

import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "score_history")
public class ScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserScore userScore;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "event_id", unique = true, length = 150)
    private String eventId;

    @Column(name = "point_change", nullable = false)
    private Integer pointChange;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private ScoreTransactionType transactionType;

    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "accumulated_before", nullable = false)
    private Integer accumulatedBefore;

    @Column(name = "accumulated_after", nullable = false)
    private Integer accumulatedAfter;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_history_id")
    private ScoreHistory referenceHistory;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "request_id", unique = true, length = 100)
    private String requestId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "requested_point_change")
    private Integer requestedPointChange;

    @Column(name = "outstanding_points", nullable = false)
    private Integer outstandingPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NONE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ScoreHistory() {
    }

    public ScoreHistory(Long id, UserScore userScore, Long bookingId, String eventId, Integer pointChange, ScoreTransactionType transactionType, Integer balanceBefore, Integer balanceAfter, Integer accumulatedBefore, Integer accumulatedAfter, String idempotencyKey, ScoreHistory referenceHistory, Long createdBy, String requestId, String reason, String description, Integer requestedPointChange, Integer outstandingPoints, ReconciliationStatus reconciliationStatus, LocalDateTime createdAt) {
        this.id = id;
        this.userScore = userScore;
        this.bookingId = bookingId;
        this.eventId = eventId;
        this.pointChange = pointChange;
        this.transactionType = transactionType;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.accumulatedBefore = accumulatedBefore;
        this.accumulatedAfter = accumulatedAfter;
        this.idempotencyKey = idempotencyKey;
        this.referenceHistory = referenceHistory;
        this.createdBy = createdBy;
        this.requestId = requestId;
        this.reason = reason;
        this.description = description;
        this.requestedPointChange = requestedPointChange;
        this.outstandingPoints = outstandingPoints != null ? outstandingPoints : 0;
        this.reconciliationStatus = reconciliationStatus != null ? reconciliationStatus : ReconciliationStatus.NONE;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserScore getUserScore() {
        return userScore;
    }

    public void setUserScore(UserScore userScore) {
        this.userScore = userScore;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Integer getPointChange() {
        return pointChange;
    }

    public void setPointChange(Integer pointChange) {
        this.pointChange = pointChange;
    }

    public ScoreTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(ScoreTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(Integer balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Integer getAccumulatedBefore() {
        return accumulatedBefore;
    }

    public void setAccumulatedBefore(Integer accumulatedBefore) {
        this.accumulatedBefore = accumulatedBefore;
    }

    public Integer getAccumulatedAfter() {
        return accumulatedAfter;
    }

    public void setAccumulatedAfter(Integer accumulatedAfter) {
        this.accumulatedAfter = accumulatedAfter;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public ScoreHistory getReferenceHistory() {
        return referenceHistory;
    }

    public void setReferenceHistory(ScoreHistory referenceHistory) {
        this.referenceHistory = referenceHistory;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRequestedPointChange() {
        return requestedPointChange;
    }

    public void setRequestedPointChange(Integer requestedPointChange) {
        this.requestedPointChange = requestedPointChange;
    }

    public Integer getOutstandingPoints() {
        return outstandingPoints;
    }

    public void setOutstandingPoints(Integer outstandingPoints) {
        this.outstandingPoints = outstandingPoints;
    }

    public ReconciliationStatus getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Builder pattern
    public static ScoreHistoryBuilder builder() {
        return new ScoreHistoryBuilder();
    }

    public static class ScoreHistoryBuilder {
        private Long id;
        private UserScore userScore;
        private Long bookingId;
        private String eventId;
        private Integer pointChange;
        private ScoreTransactionType transactionType;
        private Integer balanceBefore;
        private Integer balanceAfter;
        private Integer accumulatedBefore;
        private Integer accumulatedAfter;
        private String idempotencyKey;
        private ScoreHistory referenceHistory;
        private Long createdBy;
        private String requestId;
        private String reason;
        private String description;
        private Integer requestedPointChange;
        private Integer outstandingPoints = 0;
        private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NONE;
        private LocalDateTime createdAt;

        public ScoreHistoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ScoreHistoryBuilder userScore(UserScore userScore) {
            this.userScore = userScore;
            return this;
        }

        public ScoreHistoryBuilder bookingId(Long bookingId) {
            this.bookingId = bookingId;
            return this;
        }

        public ScoreHistoryBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public ScoreHistoryBuilder pointChange(Integer pointChange) {
            this.pointChange = pointChange;
            return this;
        }

        public ScoreHistoryBuilder transactionType(ScoreTransactionType transactionType) {
            this.transactionType = transactionType;
            return this;
        }

        public ScoreHistoryBuilder balanceBefore(Integer balanceBefore) {
            this.balanceBefore = balanceBefore;
            return this;
        }

        public ScoreHistoryBuilder balanceAfter(Integer balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public ScoreHistoryBuilder accumulatedBefore(Integer accumulatedBefore) {
            this.accumulatedBefore = accumulatedBefore;
            return this;
        }

        public ScoreHistoryBuilder accumulatedAfter(Integer accumulatedAfter) {
            this.accumulatedAfter = accumulatedAfter;
            return this;
        }

        public ScoreHistoryBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public ScoreHistoryBuilder referenceHistory(ScoreHistory referenceHistory) {
            this.referenceHistory = referenceHistory;
            return this;
        }

        public ScoreHistoryBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public ScoreHistoryBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public ScoreHistoryBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public ScoreHistoryBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ScoreHistoryBuilder requestedPointChange(Integer requestedPointChange) {
            this.requestedPointChange = requestedPointChange;
            return this;
        }

        public ScoreHistoryBuilder outstandingPoints(Integer outstandingPoints) {
            this.outstandingPoints = outstandingPoints;
            return this;
        }

        public ScoreHistoryBuilder reconciliationStatus(ReconciliationStatus reconciliationStatus) {
            this.reconciliationStatus = reconciliationStatus;
            return this;
        }

        public ScoreHistoryBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ScoreHistory build() {
            return new ScoreHistory(id, userScore, bookingId, eventId, pointChange, transactionType, balanceBefore, balanceAfter, accumulatedBefore, accumulatedAfter, idempotencyKey, referenceHistory, createdBy, requestId, reason, description, requestedPointChange, outstandingPoints, reconciliationStatus, createdAt);
        }
    }
}

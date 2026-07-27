package com.project.scoreservice.entity;

import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "score_history", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transaction_uuid", columnNames = "transaction_uuid"),
        @UniqueConstraint(name = "uk_history_event", columnNames = "event_id"),
        @UniqueConstraint(name = "uk_history_idempotency", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_history_request", columnNames = "request_id")
})
public class ScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_uuid", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String transactionUuid = java.util.UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserScore userScore;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "hold_id")
    private Long holdId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_history_id")
    private ScoreHistory referenceHistory;

    @Column(name = "event_id", length = 150)
    private String eventId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "request_id", length = 120)
    private String requestId;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "source_service", nullable = false, length = 50)
    private String sourceService;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private ScoreTransactionType transactionType;

    @Column(name = "requested_point_change")
    private Integer requestedPointChange;

    @Column(name = "actual_point_change", nullable = false)
    private Integer actualPointChange;

    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "held_before", nullable = false)
    private Integer heldBefore = 0;

    @Column(name = "held_after", nullable = false)
    private Integer heldAfter = 0;

    @Column(name = "accumulated_before", nullable = false)
    private Integer accumulatedBefore;

    @Column(name = "accumulated_after", nullable = false)
    private Integer accumulatedAfter;

    @Column(name = "outstanding_before", nullable = false)
    private Integer outstandingBefore = 0;

    @Column(name = "outstanding_after", nullable = false)
    private Integer outstandingAfter = 0;

    @Column(name = "tier_snapshot", nullable = false, length = 30)
    private String tierSnapshot;

    @Column(name = "earning_rate_snapshot", precision = 5, scale = 2)
    private BigDecimal earningRateSnapshot;

    @Column(name = "redeem_rate_snapshot", precision = 10, scale = 2)
    private BigDecimal redeemRateSnapshot;

    @Column(name = "reason")
    private String reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "approval_id")
    private Long approvalId;

    @Column(name = "case_id", length = 100)
    private String caseId;

    @Column(name = "batch_id", length = 100)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NONE;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ScoreHistory() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.transactionUuid == null) {
            this.transactionUuid = java.util.UUID.randomUUID().toString();
        }
        if (this.occurredAt == null) {
            this.occurredAt = java.time.LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionUuid() {
        return transactionUuid;
    }

    public void setTransactionUuid(String transactionUuid) {
        this.transactionUuid = transactionUuid;
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

    public Long getHoldId() {
        return holdId;
    }

    public void setHoldId(Long holdId) {
        this.holdId = holdId;
    }

    public ScoreHistory getReferenceHistory() {
        return referenceHistory;
    }

    public void setReferenceHistory(ScoreHistory referenceHistory) {
        this.referenceHistory = referenceHistory;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public ScoreTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(ScoreTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getRequestedPointChange() {
        return requestedPointChange;
    }

    public void setRequestedPointChange(Integer requestedPointChange) {
        this.requestedPointChange = requestedPointChange;
    }

    public Integer getActualPointChange() {
        return actualPointChange;
    }

    public void setActualPointChange(Integer actualPointChange) {
        this.actualPointChange = actualPointChange;
    }

    // Alias for backward compatibility if needed
    public Integer getPointChange() {
        return actualPointChange;
    }

    public void setPointChange(Integer pointChange) {
        this.actualPointChange = pointChange;
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

    public Integer getHeldBefore() {
        return heldBefore;
    }

    public void setHeldBefore(Integer heldBefore) {
        this.heldBefore = heldBefore;
    }

    public Integer getHeldAfter() {
        return heldAfter;
    }

    public void setHeldAfter(Integer heldAfter) {
        this.heldAfter = heldAfter;
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

    public Integer getOutstandingBefore() {
        return outstandingBefore;
    }

    public void setOutstandingBefore(Integer outstandingBefore) {
        this.outstandingBefore = outstandingBefore;
    }

    public Integer getOutstandingAfter() {
        return outstandingAfter;
    }

    public void setOutstandingAfter(Integer outstandingAfter) {
        this.outstandingAfter = outstandingAfter;
    }

    // Alias for backward compatibility
    public Integer getOutstandingPoints() {
        return outstandingAfter;
    }

    public void setOutstandingPoints(Integer outstandingPoints) {
        this.outstandingAfter = outstandingPoints;
    }

    public String getTierSnapshot() {
        return tierSnapshot;
    }

    public void setTierSnapshot(String tierSnapshot) {
        this.tierSnapshot = tierSnapshot;
    }

    public BigDecimal getEarningRateSnapshot() {
        return earningRateSnapshot;
    }

    public void setEarningRateSnapshot(BigDecimal earningRateSnapshot) {
        this.earningRateSnapshot = earningRateSnapshot;
    }

    public BigDecimal getRedeemRateSnapshot() {
        return redeemRateSnapshot;
    }

    public void setRedeemRateSnapshot(BigDecimal redeemRateSnapshot) {
        this.redeemRateSnapshot = redeemRateSnapshot;
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

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public Long getCreatedBy() {
        return operatorId;
    }

    public void setCreatedBy(Long createdBy) {
        this.operatorId = createdBy;
    }

    public Long getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(Long approvalId) {
        this.approvalId = approvalId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public ReconciliationStatus getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static ScoreHistoryBuilder builder() {
        return new ScoreHistoryBuilder();
    }

    public static class ScoreHistoryBuilder {
        private Long id;
        private String transactionUuid = java.util.UUID.randomUUID().toString();
        private UserScore userScore;
        private Long bookingId;
        private Long holdId;
        private ScoreHistory referenceHistory;
        private String eventId;
        private String idempotencyKey;
        private String requestId;
        private String correlationId;
        private String sourceService = "SCORE_SERVICE";
        private ScoreTransactionType transactionType;
        private Integer requestedPointChange;
        private Integer actualPointChange = 0;
        private Integer balanceBefore = 0;
        private Integer balanceAfter = 0;
        private Integer heldBefore = 0;
        private Integer heldAfter = 0;
        private Integer accumulatedBefore = 0;
        private Integer accumulatedAfter = 0;
        private Integer outstandingBefore = 0;
        private Integer outstandingAfter = 0;
        private String tierSnapshot = "SILVER";
        private BigDecimal earningRateSnapshot = BigDecimal.valueOf(0.05);
        private BigDecimal redeemRateSnapshot;
        private String reason;
        private String description;
        private Long operatorId;
        private Long approvalId;
        private String caseId;
        private String batchId;
        private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NONE;
        private String metadata;
        private LocalDateTime occurredAt = LocalDateTime.now();

        public ScoreHistoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ScoreHistoryBuilder transactionUuid(String transactionUuid) {
            this.transactionUuid = transactionUuid;
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

        public ScoreHistoryBuilder holdId(Long holdId) {
            this.holdId = holdId;
            return this;
        }

        public ScoreHistoryBuilder referenceHistory(ScoreHistory referenceHistory) {
            this.referenceHistory = referenceHistory;
            return this;
        }

        public ScoreHistoryBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public ScoreHistoryBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public ScoreHistoryBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public ScoreHistoryBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public ScoreHistoryBuilder sourceService(String sourceService) {
            this.sourceService = sourceService;
            return this;
        }

        public ScoreHistoryBuilder transactionType(ScoreTransactionType transactionType) {
            this.transactionType = transactionType;
            return this;
        }

        public ScoreHistoryBuilder requestedPointChange(Integer requestedPointChange) {
            this.requestedPointChange = requestedPointChange;
            return this;
        }

        public ScoreHistoryBuilder actualPointChange(Integer actualPointChange) {
            this.actualPointChange = actualPointChange;
            return this;
        }

        public ScoreHistoryBuilder pointChange(Integer pointChange) {
            this.actualPointChange = pointChange;
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

        public ScoreHistoryBuilder heldBefore(Integer heldBefore) {
            this.heldBefore = heldBefore;
            return this;
        }

        public ScoreHistoryBuilder heldAfter(Integer heldAfter) {
            this.heldAfter = heldAfter;
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

        public ScoreHistoryBuilder outstandingBefore(Integer outstandingBefore) {
            this.outstandingBefore = outstandingBefore;
            return this;
        }

        public ScoreHistoryBuilder outstandingAfter(Integer outstandingAfter) {
            this.outstandingAfter = outstandingAfter;
            return this;
        }

        public ScoreHistoryBuilder tierSnapshot(String tierSnapshot) {
            this.tierSnapshot = tierSnapshot;
            return this;
        }

        public ScoreHistoryBuilder earningRateSnapshot(BigDecimal earningRateSnapshot) {
            this.earningRateSnapshot = earningRateSnapshot;
            return this;
        }

        public ScoreHistoryBuilder redeemRateSnapshot(BigDecimal redeemRateSnapshot) {
            this.redeemRateSnapshot = redeemRateSnapshot;
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

        public ScoreHistoryBuilder operatorId(Long operatorId) {
            this.operatorId = operatorId;
            return this;
        }

        public ScoreHistoryBuilder createdBy(Long createdBy) {
            this.operatorId = createdBy;
            return this;
        }

        public ScoreHistoryBuilder approvalId(Long approvalId) {
            this.approvalId = approvalId;
            return this;
        }

        public ScoreHistoryBuilder caseId(String caseId) {
            this.caseId = caseId;
            return this;
        }

        public ScoreHistoryBuilder batchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        public ScoreHistoryBuilder reconciliationStatus(ReconciliationStatus reconciliationStatus) {
            this.reconciliationStatus = reconciliationStatus;
            return this;
        }

        public ScoreHistoryBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public ScoreHistoryBuilder occurredAt(LocalDateTime occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public ScoreHistoryBuilder outstandingPoints(Integer outstandingPoints) {
            this.outstandingAfter = outstandingPoints;
            return this;
        }

        public ScoreHistory build() {
            ScoreHistory sh = new ScoreHistory();
            sh.id = this.id;
            sh.transactionUuid = this.transactionUuid;
            sh.userScore = this.userScore;
            sh.bookingId = this.bookingId;
            sh.holdId = this.holdId;
            sh.referenceHistory = this.referenceHistory;
            sh.eventId = this.eventId;
            sh.idempotencyKey = this.idempotencyKey;
            sh.requestId = this.requestId;
            sh.correlationId = this.correlationId;
            sh.sourceService = this.sourceService;
            sh.transactionType = this.transactionType;
            sh.requestedPointChange = this.requestedPointChange;
            sh.actualPointChange = this.actualPointChange;
            sh.balanceBefore = this.balanceBefore;
            sh.balanceAfter = this.balanceAfter;
            sh.heldBefore = this.heldBefore;
            sh.heldAfter = this.heldAfter;
            sh.accumulatedBefore = this.accumulatedBefore;
            sh.accumulatedAfter = this.accumulatedAfter;
            sh.outstandingBefore = this.outstandingBefore;
            sh.outstandingAfter = this.outstandingAfter;
            sh.tierSnapshot = this.tierSnapshot;
            sh.earningRateSnapshot = this.earningRateSnapshot;
            sh.redeemRateSnapshot = this.redeemRateSnapshot;
            sh.reason = this.reason;
            sh.description = this.description;
            sh.operatorId = this.operatorId;
            sh.approvalId = this.approvalId;
            sh.caseId = this.caseId;
            sh.batchId = this.batchId;
            sh.reconciliationStatus = this.reconciliationStatus;
            sh.metadata = this.metadata;
            sh.occurredAt = this.occurredAt != null ? this.occurredAt : LocalDateTime.now();
            return sh;
        }
    }
}

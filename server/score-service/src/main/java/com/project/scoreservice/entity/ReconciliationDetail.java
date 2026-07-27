package com.project.scoreservice.entity;

import com.project.scoreservice.enumtype.ReconciliationDetailStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_details")
public class ReconciliationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ReconciliationRun run;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "current_balance", nullable = false)
    private Integer currentBalance;

    @Column(name = "ledger_balance", nullable = false)
    private Integer ledgerBalance;

    @Column(name = "balance_difference", nullable = false)
    private Integer balanceDifference;

    @Column(name = "current_held_points", nullable = false)
    private Integer currentHeldPoints;

    @Column(name = "ledger_held_points", nullable = false)
    private Integer ledgerHeldPoints;

    @Column(name = "held_difference", nullable = false)
    private Integer heldDifference;

    @Column(name = "current_accumulated", nullable = false)
    private Integer currentAccumulated;

    @Column(name = "ledger_accumulated", nullable = false)
    private Integer ledgerAccumulated;

    @Column(name = "accumulated_difference", nullable = false)
    private Integer accumulatedDifference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReconciliationDetailStatus status;

    @Column(name = "adjustment_history_id")
    private Long adjustmentHistoryId;

    @Column(name = "remark", length = 500)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ReconciliationDetail() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReconciliationRun getRun() { return run; }
    public void setRun(ReconciliationRun run) { this.run = run; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(Integer currentBalance) { this.currentBalance = currentBalance; }

    public Integer getLedgerBalance() { return ledgerBalance; }
    public void setLedgerBalance(Integer ledgerBalance) { this.ledgerBalance = ledgerBalance; }

    public Integer getBalanceDifference() { return balanceDifference; }
    public void setBalanceDifference(Integer balanceDifference) { this.balanceDifference = balanceDifference; }

    public Integer getCurrentHeldPoints() { return currentHeldPoints; }
    public void setCurrentHeldPoints(Integer currentHeldPoints) { this.currentHeldPoints = currentHeldPoints; }

    public Integer getLedgerHeldPoints() { return ledgerHeldPoints; }
    public void setLedgerHeldPoints(Integer ledgerHeldPoints) { this.ledgerHeldPoints = ledgerHeldPoints; }

    public Integer getHeldDifference() { return heldDifference; }
    public void setHeldDifference(Integer heldDifference) { this.heldDifference = heldDifference; }

    public Integer getCurrentAccumulated() { return currentAccumulated; }
    public void setCurrentAccumulated(Integer currentAccumulated) { this.currentAccumulated = currentAccumulated; }

    public Integer getLedgerAccumulated() { return ledgerAccumulated; }
    public void setLedgerAccumulated(Integer ledgerAccumulated) { this.ledgerAccumulated = ledgerAccumulated; }

    public Integer getAccumulatedDifference() { return accumulatedDifference; }
    public void setAccumulatedDifference(Integer accumulatedDifference) { this.accumulatedDifference = accumulatedDifference; }

    public ReconciliationDetailStatus getStatus() { return status; }
    public void setStatus(ReconciliationDetailStatus status) { this.status = status; }

    public Long getAdjustmentHistoryId() { return adjustmentHistoryId; }
    public void setAdjustmentHistoryId(Long adjustmentHistoryId) { this.adjustmentHistoryId = adjustmentHistoryId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

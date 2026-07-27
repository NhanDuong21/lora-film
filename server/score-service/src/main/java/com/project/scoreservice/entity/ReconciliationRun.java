package com.project.scoreservice.entity;

import com.project.scoreservice.enumtype.ReconciliationRunStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_runs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reconciliation_batch", columnNames = "batch_code")
})
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_code", nullable = false, length = 100, unique = true)
    private String batchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReconciliationRunStatus status = ReconciliationRunStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "total_users", nullable = false)
    private Integer totalUsers = 0;

    @Column(name = "matched_users", nullable = false)
    private Integer matchedUsers = 0;

    @Column(name = "mismatched_users", nullable = false)
    private Integer mismatchedUsers = 0;

    @Column(name = "total_adjustments", nullable = false)
    private Integer totalAdjustments = 0;

    @Column(name = "executed_by")
    private Long executedBy;

    @Column(name = "remark", length = 500)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ReconciliationRun() {
    }

    public ReconciliationRun(String batchCode, ReconciliationRunStatus status, LocalDateTime startedAt, Long executedBy, String remark) {
        this.batchCode = batchCode;
        this.status = status;
        this.startedAt = startedAt != null ? startedAt : LocalDateTime.now();
        this.executedBy = executedBy;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public ReconciliationRunStatus getStatus() { return status; }
    public void setStatus(ReconciliationRunStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Integer getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Integer totalUsers) { this.totalUsers = totalUsers; }

    public Integer getMatchedUsers() { return matchedUsers; }
    public void setMatchedUsers(Integer matchedUsers) { this.matchedUsers = matchedUsers; }

    public Integer getMismatchedUsers() { return mismatchedUsers; }
    public void setMismatchedUsers(Integer mismatchedUsers) { this.mismatchedUsers = mismatchedUsers; }

    public Integer getTotalAdjustments() { return totalAdjustments; }
    public void setTotalAdjustments(Integer totalAdjustments) { this.totalAdjustments = totalAdjustments; }

    public Long getExecutedBy() { return executedBy; }
    public void setExecutedBy(Long executedBy) { this.executedBy = executedBy; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

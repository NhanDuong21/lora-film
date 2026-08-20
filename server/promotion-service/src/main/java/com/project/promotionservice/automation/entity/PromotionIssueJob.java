package com.project.promotionservice.automation.entity;

import com.project.promotionservice.automation.enums.IssueJobStatus;
import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "promotion_issue_jobs")
public class PromotionIssueJob extends BaseAuditableEntity {
    @Column(name = "run_public_id", nullable = false, length = 36)
    private String runPublicId;
    @Column(name = "snapshot_public_id", nullable = false, length = 36)
    private String snapshotPublicId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IssueJobStatus status = IssueJobStatus.PENDING;
    @Column(name = "batch_size", nullable = false)
    private Integer batchSize = 200;
    @Column(name = "processed_count", nullable = false)
    private Integer processedCount = 0;
    @Column(name = "issued_count", nullable = false)
    private Integer issuedCount = 0;
    @Column(name = "skipped_count", nullable = false)
    private Integer skippedCount = 0;
    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;
    @Column(name = "last_error", length = 500)
    private String lastError;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    public String getRunPublicId() { return runPublicId; }
    public void setRunPublicId(String value) { runPublicId = value; }
    public String getSnapshotPublicId() { return snapshotPublicId; }
    public void setSnapshotPublicId(String value) { snapshotPublicId = value; }
    public IssueJobStatus getStatus() { return status; }
    public void setStatus(IssueJobStatus value) { status = value; }
    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer value) { batchSize = value; }
    public Integer getProcessedCount() { return processedCount; }
    public void setProcessedCount(Integer value) { processedCount = value; }
    public Integer getIssuedCount() { return issuedCount; }
    public void setIssuedCount(Integer value) { issuedCount = value; }
    public Integer getSkippedCount() { return skippedCount; }
    public void setSkippedCount(Integer value) { skippedCount = value; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer value) { failedCount = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant value) { startedAt = value; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant value) { completedAt = value; }
}

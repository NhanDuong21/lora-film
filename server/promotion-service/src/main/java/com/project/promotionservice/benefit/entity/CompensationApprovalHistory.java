package com.project.promotionservice.benefit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "approval_histories")
public class CompensationApprovalHistory extends BenefitAuditableRecord {

    @Column(name = "target_type", length = 50, nullable = false)
    private String targetType;

    @Column(name = "target_public_id", length = 36, nullable = false)
    private String targetPublicId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "old_status", length = 30)
    private String oldStatus;

    @Column(name = "new_status", length = 30, nullable = false)
    private String newStatus;

    @Column(name = "approver_public_id", length = 36, nullable = false)
    private String approverPublicId;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    public CompensationApprovalHistory() {
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetPublicId() {
        return targetPublicId;
    }

    public void setTargetPublicId(String targetPublicId) {
        this.targetPublicId = targetPublicId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getApproverPublicId() {
        return approverPublicId;
    }

    public void setApproverPublicId(String approverPublicId) {
        this.approverPublicId = approverPublicId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}

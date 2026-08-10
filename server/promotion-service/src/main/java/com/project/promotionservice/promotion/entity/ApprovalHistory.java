package com.project.promotionservice.promotion.entity;

import com.project.promotionservice.promotion.enums.ApprovalAction;
import com.project.promotionservice.promotion.enums.ApprovalTargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_histories")
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50, nullable = false)
    private ApprovalTargetType targetType;

    @Column(name = "target_public_id", length = 36, nullable = false)
    private String targetPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 50, nullable = false)
    private ApprovalAction action;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 36)
    private String deletedBy;

    public ApprovalHistory() {
        this.publicId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public ApprovalTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(ApprovalTargetType targetType) {
        this.targetType = targetType;
    }

    public String getTargetPublicId() {
        return targetPublicId;
    }

    public void setTargetPublicId(String targetPublicId) {
        this.targetPublicId = targetPublicId;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public void setAction(ApprovalAction action) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }
}

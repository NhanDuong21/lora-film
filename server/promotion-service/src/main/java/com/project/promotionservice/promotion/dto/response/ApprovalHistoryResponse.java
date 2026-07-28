package com.project.promotionservice.promotion.dto.response;

import com.project.promotionservice.promotion.enums.ApprovalAction;
import com.project.promotionservice.promotion.enums.ApprovalTargetType;

import java.time.Instant;

public class ApprovalHistoryResponse {

    private String publicId;
    private ApprovalTargetType targetType;
    private String targetPublicId;
    private ApprovalAction action;
    private String oldStatus;
    private String newStatus;
    private String approverPublicId;
    private String comment;
    private Instant approvedAt;
    private String metadataJson;
    private Instant createdAt;
    private String createdBy;

    public ApprovalHistoryResponse() {
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
}

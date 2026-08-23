package com.project.promotionservice.automation.entity;

import com.project.promotionservice.automation.enums.AnomalyCaseStatus;
import com.project.promotionservice.automation.enums.AnomalyResolution;
import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_anomaly_cases", uniqueConstraints =
        @UniqueConstraint(name = "uq_promotion_anomaly_member",
                columnNames = "audience_member_public_id"))
public class PromotionAnomalyCase extends BaseAuditableEntity {
    @Column(name = "run_public_id", nullable = false, length = 36)
    private String runPublicId;
    @Column(name = "audience_member_public_id", nullable = false, length = 36)
    private String audienceMemberPublicId;
    @Column(name = "playbook_code", nullable = false, length = 80)
    private String playbookCode;
    @Column(name = "customer_public_id", nullable = false, length = 36)
    private String customerPublicId;
    @Column(name = "source_reference", length = 180)
    private String sourceReference;
    @Column(name = "reason_code", nullable = false, length = 100)
    private String reasonCode;
    @Column(name = "cost_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;
    @Column(name = "test_data", nullable = false)
    private Boolean testData = false;
    @Column(name = "environment_tag", nullable = false, length = 30)
    private String environmentTag = "BUSINESS";
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AnomalyCaseStatus status = AnomalyCaseStatus.OPEN;
    @Column(name = "assigned_to", length = 36)
    private String assignedTo;
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", length = 40)
    private AnomalyResolution resolution;
    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;
    @Column(name = "resolved_by", length = 36)
    private String resolvedBy;
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public String getRunPublicId() { return runPublicId; }
    public void setRunPublicId(String value) { runPublicId = value; }
    public String getAudienceMemberPublicId() { return audienceMemberPublicId; }
    public void setAudienceMemberPublicId(String value) { audienceMemberPublicId = value; }
    public String getPlaybookCode() { return playbookCode; }
    public void setPlaybookCode(String value) { playbookCode = value; }
    public String getCustomerPublicId() { return customerPublicId; }
    public void setCustomerPublicId(String value) { customerPublicId = value; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String value) { sourceReference = value; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String value) { reasonCode = value; }
    public BigDecimal getCostAmount() { return costAmount; }
    public void setCostAmount(BigDecimal value) { costAmount = value; }
    public Boolean getTestData() { return testData; }
    public void setTestData(Boolean value) { testData = value; }
    public String getEnvironmentTag() { return environmentTag; }
    public void setEnvironmentTag(String value) { environmentTag = value; }
    public AnomalyCaseStatus getStatus() { return status; }
    public void setStatus(AnomalyCaseStatus value) { status = value; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String value) { assignedTo = value; }
    public AnomalyResolution getResolution() { return resolution; }
    public void setResolution(AnomalyResolution value) { resolution = value; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String value) { resolutionNote = value; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String value) { resolvedBy = value; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant value) { resolvedAt = value; }
}

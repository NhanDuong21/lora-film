package com.project.promotionservice.automation.entity;

import com.project.promotionservice.automation.enums.AudienceMemberStatus;
import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "promotion_audience_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_audience_snapshot_customer",
                columnNames = {"snapshot_public_id", "customer_public_id"}),
        @UniqueConstraint(name = "uq_audience_run_customer",
                columnNames = {"run_public_id", "customer_public_id"})
})
public class PromotionAudienceMember extends BaseAuditableEntity {
    @Column(name = "snapshot_public_id", nullable = false, length = 36)
    private String snapshotPublicId;
    @Column(name = "run_public_id", nullable = false, length = 36)
    private String runPublicId;
    @Column(name = "customer_public_id", nullable = false, length = 36)
    private String customerPublicId;
    @Column(name = "test_data", nullable = false)
    private Boolean testData = false;
    @Column(name = "environment_tag", nullable = false, length = 30)
    private String environmentTag = "BUSINESS";
    @Column(name = "issuance_key", nullable = false, length = 180)
    private String issuanceKey;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AudienceMemberStatus status = AudienceMemberStatus.PENDING;
    @Column(name = "reason_code", length = 80)
    private String reasonCode;
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;
    @Column(name = "wallet_public_id", length = 36)
    private String walletPublicId;
    @Column(name = "budget_reserved_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetReservedAmount = BigDecimal.ZERO;
    @Column(name = "budget_period_key", length = 7)
    private String budgetPeriodKey;

    public String getSnapshotPublicId() { return snapshotPublicId; }
    public void setSnapshotPublicId(String value) { snapshotPublicId = value; }
    public String getRunPublicId() { return runPublicId; }
    public void setRunPublicId(String value) { runPublicId = value; }
    public String getCustomerPublicId() { return customerPublicId; }
    public void setCustomerPublicId(String value) { customerPublicId = value; }
    public Boolean getTestData() { return testData; }
    public void setTestData(Boolean value) { testData = value; }
    public String getEnvironmentTag() { return environmentTag; }
    public void setEnvironmentTag(String value) { environmentTag = value; }
    public String getIssuanceKey() { return issuanceKey; }
    public void setIssuanceKey(String value) { issuanceKey = value; }
    public AudienceMemberStatus getStatus() { return status; }
    public void setStatus(AudienceMemberStatus value) { status = value; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String value) { reasonCode = value; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer value) { attemptCount = value; }
    public String getWalletPublicId() { return walletPublicId; }
    public void setWalletPublicId(String value) { walletPublicId = value; }
    public BigDecimal getBudgetReservedAmount() { return budgetReservedAmount; }
    public void setBudgetReservedAmount(BigDecimal value) { budgetReservedAmount = value; }
    public String getBudgetPeriodKey() { return budgetPeriodKey; }
    public void setBudgetPeriodKey(String value) { budgetPeriodKey = value; }
}

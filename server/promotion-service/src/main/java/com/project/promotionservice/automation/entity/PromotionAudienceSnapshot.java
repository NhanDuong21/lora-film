package com.project.promotionservice.automation.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "promotion_audience_snapshots")
public class PromotionAudienceSnapshot extends BaseAuditableEntity {
    @Column(name = "run_public_id", nullable = false, unique = true, length = 36)
    private String runPublicId;
    @Column(name = "test_data", nullable = false)
    private Boolean testData = false;
    @Column(name = "environment_tag", nullable = false, length = 30)
    private String environmentTag = "BUSINESS";
    @Column(name = "audience_rule_snapshot_json", nullable = false, columnDefinition = "json")
    private String audienceRuleSnapshotJson;
    @Column(name = "total_count", nullable = false)
    private Integer totalCount = 0;
    @Column(name = "eligible_count", nullable = false)
    private Integer eligibleCount = 0;
    @Column(name = "excluded_count", nullable = false)
    private Integer excludedCount = 0;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    public String getRunPublicId() { return runPublicId; }
    public void setRunPublicId(String value) { runPublicId = value; }
    public Boolean getTestData() { return testData; }
    public void setTestData(Boolean value) { testData = value; }
    public String getEnvironmentTag() { return environmentTag; }
    public void setEnvironmentTag(String value) { environmentTag = value; }
    public String getAudienceRuleSnapshotJson() { return audienceRuleSnapshotJson; }
    public void setAudienceRuleSnapshotJson(String value) { audienceRuleSnapshotJson = value; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer value) { totalCount = value; }
    public Integer getEligibleCount() { return eligibleCount; }
    public void setEligibleCount(Integer value) { eligibleCount = value; }
    public Integer getExcludedCount() { return excludedCount; }
    public void setExcludedCount(Integer value) { excludedCount = value; }
    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant value) { capturedAt = value; }
}

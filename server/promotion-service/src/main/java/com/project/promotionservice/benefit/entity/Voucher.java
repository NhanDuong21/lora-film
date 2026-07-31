package com.project.promotionservice.benefit.entity;

import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherType;
import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vouchers")
public class Voucher extends BaseAuditableEntity {

    @Column(name = "campaign_public_id", length = 36)
    private String campaignPublicId;

    @Column(name = "owner_public_id", length = 36, nullable = false)
    private String ownerPublicId;

    @Column(name = "code", length = 100, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_type", length = 50, nullable = false)
    private VoucherType voucherType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 50, nullable = false)
    private VoucherSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private VoucherStatus status;

    @Column(name = "issue_reason", length = 255)
    private String issueReason;

    @Column(name = "issued_by", length = 36)
    private String issuedBy;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(name = "transferable", nullable = false)
    private Boolean transferable = false;

    @Column(name = "stackable", nullable = false)
    private Boolean stackable = false;

    @Column(name = "reusable", nullable = false)
    private Boolean reusable = false;

    @Column(name = "max_usage", nullable = false)
    private Integer maxUsage = 1;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "face_value", precision = 18, scale = 2)
    private BigDecimal faceValue;

    @Column(name = "minimum_order_amount", precision = 18, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(name = "conditions_json", columnDefinition = "JSON", nullable = false)
    private String conditionsJson;

    @Column(name = "actions_json", columnDefinition = "JSON", nullable = false)
    private String actionsJson;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    public Voucher() {
    }

    public String getCampaignPublicId() {
        return campaignPublicId;
    }

    public void setCampaignPublicId(String campaignPublicId) {
        this.campaignPublicId = campaignPublicId;
    }

    public String getOwnerPublicId() {
        return ownerPublicId;
    }

    public void setOwnerPublicId(String ownerPublicId) {
        this.ownerPublicId = ownerPublicId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VoucherType getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(VoucherType voucherType) {
        this.voucherType = voucherType;
    }

    public VoucherSource getSource() {
        return source;
    }

    public void setSource(VoucherSource source) {
        this.source = source;
    }

    public VoucherStatus getStatus() {
        return status;
    }

    public void setStatus(VoucherStatus status) {
        this.status = status;
    }

    public String getIssueReason() {
        return issueReason;
    }

    public void setIssueReason(String issueReason) {
        this.issueReason = issueReason;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }

    public Boolean getTransferable() {
        return transferable;
    }

    public void setTransferable(Boolean transferable) {
        this.transferable = transferable;
    }

    public Boolean getStackable() {
        return stackable;
    }

    public void setStackable(Boolean stackable) {
        this.stackable = stackable;
    }

    public Boolean getReusable() {
        return reusable;
    }

    public void setReusable(Boolean reusable) {
        this.reusable = reusable;
    }

    public Integer getMaxUsage() {
        return maxUsage;
    }

    public void setMaxUsage(Integer maxUsage) {
        this.maxUsage = maxUsage;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public void setFaceValue(BigDecimal faceValue) {
        this.faceValue = faceValue;
    }

    public BigDecimal getMinimumOrderAmount() {
        return minimumOrderAmount;
    }

    public void setMinimumOrderAmount(BigDecimal minimumOrderAmount) {
        this.minimumOrderAmount = minimumOrderAmount;
    }

    public String getConditionsJson() {
        return conditionsJson;
    }

    public void setConditionsJson(String conditionsJson) {
        this.conditionsJson = conditionsJson;
    }

    public String getActionsJson() {
        return actionsJson;
    }

    public void setActionsJson(String actionsJson) {
        this.actionsJson = actionsJson;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}

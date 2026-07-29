package com.project.promotionservice.partner.dto.response;

import com.project.promotionservice.partner.enums.SettlementRule;
import com.project.promotionservice.partner.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class SettlementResponse {
    private String publicId;
    private String partnerPublicId;
    private String campaignPublicId;
    private String settlementCode;
    private Instant settlementPeriodFrom;
    private Instant settlementPeriodTo;
    private Integer totalOrders;
    private BigDecimal totalDiscount;
    private BigDecimal partnerAmount;
    private BigDecimal platformAmount;
    private BigDecimal adjustmentAmount;
    private BigDecimal finalAmount;
    private String currency;
    private SettlementRule settlementRule;
    private BigDecimal partnerPercentage;
    private BigDecimal fixedAmountPerRedemption;
    private SettlementStatus status;
    private Instant approvedAt;
    private Instant paidAt;
    private String note;
    private String metadataJson;
    private Integer version;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getPartnerPublicId() { return partnerPublicId; }
    public void setPartnerPublicId(String partnerPublicId) { this.partnerPublicId = partnerPublicId; }
    public String getCampaignPublicId() { return campaignPublicId; }
    public void setCampaignPublicId(String campaignPublicId) { this.campaignPublicId = campaignPublicId; }
    public String getSettlementCode() { return settlementCode; }
    public void setSettlementCode(String settlementCode) { this.settlementCode = settlementCode; }
    public Instant getSettlementPeriodFrom() { return settlementPeriodFrom; }
    public void setSettlementPeriodFrom(Instant settlementPeriodFrom) { this.settlementPeriodFrom = settlementPeriodFrom; }
    public Instant getSettlementPeriodTo() { return settlementPeriodTo; }
    public void setSettlementPeriodTo(Instant settlementPeriodTo) { this.settlementPeriodTo = settlementPeriodTo; }
    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    public BigDecimal getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; }
    public BigDecimal getPartnerAmount() { return partnerAmount; }
    public void setPartnerAmount(BigDecimal partnerAmount) { this.partnerAmount = partnerAmount; }
    public BigDecimal getPlatformAmount() { return platformAmount; }
    public void setPlatformAmount(BigDecimal platformAmount) { this.platformAmount = platformAmount; }
    public BigDecimal getAdjustmentAmount() { return adjustmentAmount; }
    public void setAdjustmentAmount(BigDecimal adjustmentAmount) { this.adjustmentAmount = adjustmentAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public SettlementRule getSettlementRule() { return settlementRule; }
    public void setSettlementRule(SettlementRule settlementRule) { this.settlementRule = settlementRule; }
    public BigDecimal getPartnerPercentage() { return partnerPercentage; }
    public void setPartnerPercentage(BigDecimal partnerPercentage) { this.partnerPercentage = partnerPercentage; }
    public BigDecimal getFixedAmountPerRedemption() { return fixedAmountPerRedemption; }
    public void setFixedAmountPerRedemption(BigDecimal fixedAmountPerRedemption) { this.fixedAmountPerRedemption = fixedAmountPerRedemption; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}

package com.project.promotionservice.partner.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import com.project.promotionservice.partner.enums.SettlementRule;
import com.project.promotionservice.partner.enums.SettlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "partner_settlements")
public class PartnerSettlement extends BaseAuditableEntity {

    @Column(name = "partner_public_id", length = 36, nullable = false)
    private String partnerPublicId;

    @Column(name = "campaign_public_id", length = 36)
    private String campaignPublicId;

    @Column(name = "settlement_code", length = 100, nullable = false, unique = true)
    private String settlementCode;

    @Column(name = "settlement_period_from", nullable = false)
    private Instant settlementPeriodFrom;

    @Column(name = "settlement_period_to", nullable = false)
    private Instant settlementPeriodTo;

    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders = 0;

    @Column(name = "total_discount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "partner_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal partnerAmount = BigDecimal.ZERO;

    @Column(name = "platform_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal platformAmount = BigDecimal.ZERO;

    @Column(name = "adjustment_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_rule", length = 50, nullable = false)
    private SettlementRule settlementRule = SettlementRule.PERCENTAGE_OF_DISCOUNT;

    @Column(name = "partner_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal partnerPercentage = BigDecimal.ZERO;

    @Column(name = "fixed_amount_per_redemption", precision = 18, scale = 2)
    private BigDecimal fixedAmountPerRedemption;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private SettlementStatus status = SettlementStatus.PENDING_APPROVAL;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    public String getPartnerPublicId() {
        return partnerPublicId;
    }

    public void setPartnerPublicId(String partnerPublicId) {
        this.partnerPublicId = partnerPublicId;
    }

    public String getCampaignPublicId() {
        return campaignPublicId;
    }

    public void setCampaignPublicId(String campaignPublicId) {
        this.campaignPublicId = campaignPublicId;
    }

    public String getSettlementCode() {
        return settlementCode;
    }

    public void setSettlementCode(String settlementCode) {
        this.settlementCode = settlementCode;
    }

    public Instant getSettlementPeriodFrom() {
        return settlementPeriodFrom;
    }

    public void setSettlementPeriodFrom(Instant settlementPeriodFrom) {
        this.settlementPeriodFrom = settlementPeriodFrom;
    }

    public Instant getSettlementPeriodTo() {
        return settlementPeriodTo;
    }

    public void setSettlementPeriodTo(Instant settlementPeriodTo) {
        this.settlementPeriodTo = settlementPeriodTo;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public BigDecimal getPartnerAmount() {
        return partnerAmount;
    }

    public void setPartnerAmount(BigDecimal partnerAmount) {
        this.partnerAmount = partnerAmount;
    }

    public BigDecimal getPlatformAmount() {
        return platformAmount;
    }

    public void setPlatformAmount(BigDecimal platformAmount) {
        this.platformAmount = platformAmount;
    }

    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public void setAdjustmentAmount(BigDecimal adjustmentAmount) {
        this.adjustmentAmount = adjustmentAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public SettlementRule getSettlementRule() {
        return settlementRule;
    }

    public void setSettlementRule(SettlementRule settlementRule) {
        this.settlementRule = settlementRule;
    }

    public BigDecimal getPartnerPercentage() {
        return partnerPercentage;
    }

    public void setPartnerPercentage(BigDecimal partnerPercentage) {
        this.partnerPercentage = partnerPercentage;
    }

    public BigDecimal getFixedAmountPerRedemption() {
        return fixedAmountPerRedemption;
    }

    public void setFixedAmountPerRedemption(BigDecimal fixedAmountPerRedemption) {
        this.fixedAmountPerRedemption = fixedAmountPerRedemption;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}

package com.project.promotionservice.partner.dto.request;

import com.project.promotionservice.partner.enums.SettlementRule;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

public class SettlementCreateRequest {

    @NotBlank
    @Pattern(regexp = UUID_PATTERN)
    private String partnerPublicId;

    @Pattern(regexp = UUID_PATTERN)
    private String campaignPublicId;

    @NotNull
    private Instant settlementPeriodFrom;

    @NotNull
    private Instant settlementPeriodTo;

    @NotNull
    private SettlementRule settlementRule = SettlementRule.PERCENTAGE_OF_DISCOUNT;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal partnerPercentage = BigDecimal.ZERO;

    @DecimalMin("0.00")
    private BigDecimal fixedAmountPerRedemption;

    @NotBlank
    @Size(min = 3, max = 10)
    @Pattern(regexp = "^[A-Z]{3,10}$")
    private String currency = "VND";

    @Size(max = 1000)
    private String note;

    public String getPartnerPublicId() { return partnerPublicId; }
    public void setPartnerPublicId(String partnerPublicId) { this.partnerPublicId = partnerPublicId; }
    public String getCampaignPublicId() { return campaignPublicId; }
    public void setCampaignPublicId(String campaignPublicId) { this.campaignPublicId = campaignPublicId; }
    public Instant getSettlementPeriodFrom() { return settlementPeriodFrom; }
    public void setSettlementPeriodFrom(Instant settlementPeriodFrom) { this.settlementPeriodFrom = settlementPeriodFrom; }
    public Instant getSettlementPeriodTo() { return settlementPeriodTo; }
    public void setSettlementPeriodTo(Instant settlementPeriodTo) { this.settlementPeriodTo = settlementPeriodTo; }
    public SettlementRule getSettlementRule() { return settlementRule; }
    public void setSettlementRule(SettlementRule settlementRule) { this.settlementRule = settlementRule; }
    public BigDecimal getPartnerPercentage() { return partnerPercentage; }
    public void setPartnerPercentage(BigDecimal partnerPercentage) { this.partnerPercentage = partnerPercentage; }
    public BigDecimal getFixedAmountPerRedemption() { return fixedAmountPerRedemption; }
    public void setFixedAmountPerRedemption(BigDecimal fixedAmountPerRedemption) { this.fixedAmountPerRedemption = fixedAmountPerRedemption; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

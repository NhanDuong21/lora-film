package com.lorafilm.movie.pricing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public class ShowtimePriceDto {
    private String seatTypeId;
    private String seatTypeName;
    private String seatTypeCode;
    private BigDecimal price;
    private String pricingSource;
    private String sourcePolicyId;
    private String sourcePolicyName;
    private String sourceRuleId;
    private Instant resolvedAt;
    private String resolutionTimezone;

    public ShowtimePriceDto() {}

    public ShowtimePriceDto(String seatTypeId, BigDecimal price) {
        this.seatTypeId = seatTypeId;
        this.price = price;
    }

    public ShowtimePriceDto(String seatTypeId, String seatTypeName, String seatTypeCode, BigDecimal price) {
        this.seatTypeId = seatTypeId;
        this.seatTypeName = seatTypeName;
        this.seatTypeCode = seatTypeCode;
        this.price = price;
    }

    public String getSeatTypeId() {
        return seatTypeId;
    }

    public void setSeatTypeId(String seatTypeId) {
        this.seatTypeId = seatTypeId;
    }

    public String getSeatTypeName() {
        return seatTypeName;
    }

    public void setSeatTypeName(String seatTypeName) {
        this.seatTypeName = seatTypeName;
    }

    public String getSeatTypeCode() {
        return seatTypeCode;
    }

    public void setSeatTypeCode(String seatTypeCode) {
        this.seatTypeCode = seatTypeCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPricingSource() { return pricingSource; }
    public void setPricingSource(String pricingSource) { this.pricingSource = pricingSource; }
    public String getSourcePolicyId() { return sourcePolicyId; }
    public void setSourcePolicyId(String sourcePolicyId) { this.sourcePolicyId = sourcePolicyId; }
    public String getSourcePolicyName() { return sourcePolicyName; }
    public void setSourcePolicyName(String sourcePolicyName) { this.sourcePolicyName = sourcePolicyName; }
    public String getSourceRuleId() { return sourceRuleId; }
    public void setSourceRuleId(String sourceRuleId) { this.sourceRuleId = sourceRuleId; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolutionTimezone() { return resolutionTimezone; }
    public void setResolutionTimezone(String resolutionTimezone) { this.resolutionTimezone = resolutionTimezone; }
}

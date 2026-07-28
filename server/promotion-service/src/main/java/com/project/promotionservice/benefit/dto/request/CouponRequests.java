package com.project.promotionservice.benefit.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponType;
import com.project.promotionservice.benefit.enums.BenefitEnums.DistributionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CouponRequests {

    private CouponRequests() {
    }

    public abstract static class CouponPayload {

        @NotBlank
        @Size(max = 255)
        private String name;

        private String description;

        @NotNull
        private CouponType couponType;

        private CouponStatus status = CouponStatus.ACTIVE;

        @NotNull
        private DistributionType distributionType;

        private Boolean stackable = false;
        private Boolean transferable = false;
        private Boolean reusable = false;
        private Boolean autoApply = false;

        @Min(0)
        private Integer priority = 100;

        @Min(1)
        private Integer maxRedemptions;

        @NotNull
        @Min(1)
        private Integer maxRedemptionsPerUser = 1;

        @NotNull
        private Instant validFrom;

        @NotNull
        private Instant validTo;

        @NotNull
        private JsonNode conditionsJson;

        @NotNull
        private JsonNode actionsJson;

        private JsonNode metadataJson;

        @AssertTrue(message = "validTo must be after validFrom")
        public boolean isPeriodValid() {
            return validFrom == null || validTo == null || validTo.isAfter(validFrom);
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

        public CouponType getCouponType() {
            return couponType;
        }

        public void setCouponType(CouponType couponType) {
            this.couponType = couponType;
        }

        public CouponStatus getStatus() {
            return status;
        }

        public void setStatus(CouponStatus status) {
            this.status = status;
        }

        public DistributionType getDistributionType() {
            return distributionType;
        }

        public void setDistributionType(DistributionType distributionType) {
            this.distributionType = distributionType;
        }

        public Boolean getStackable() {
            return stackable;
        }

        public void setStackable(Boolean stackable) {
            this.stackable = stackable;
        }

        public Boolean getTransferable() {
            return transferable;
        }

        public void setTransferable(Boolean transferable) {
            this.transferable = transferable;
        }

        public Boolean getReusable() {
            return reusable;
        }

        public void setReusable(Boolean reusable) {
            this.reusable = reusable;
        }

        public Boolean getAutoApply() {
            return autoApply;
        }

        public void setAutoApply(Boolean autoApply) {
            this.autoApply = autoApply;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public Integer getMaxRedemptions() {
            return maxRedemptions;
        }

        public void setMaxRedemptions(Integer maxRedemptions) {
            this.maxRedemptions = maxRedemptions;
        }

        public Integer getMaxRedemptionsPerUser() {
            return maxRedemptionsPerUser;
        }

        public void setMaxRedemptionsPerUser(Integer maxRedemptionsPerUser) {
            this.maxRedemptionsPerUser = maxRedemptionsPerUser;
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

        public JsonNode getConditionsJson() {
            return conditionsJson;
        }

        public void setConditionsJson(JsonNode conditionsJson) {
            this.conditionsJson = conditionsJson;
        }

        public JsonNode getActionsJson() {
            return actionsJson;
        }

        public void setActionsJson(JsonNode actionsJson) {
            this.actionsJson = actionsJson;
        }

        public JsonNode getMetadataJson() {
            return metadataJson;
        }

        public void setMetadataJson(JsonNode metadataJson) {
            this.metadataJson = metadataJson;
        }
    }

    public static class CouponCreateRequest extends CouponPayload {

        @NotBlank
        @Size(max = 36)
        private String campaignPublicId;

        @NotBlank
        @Size(max = 100)
        private String code;

        public String getCampaignPublicId() {
            return campaignPublicId;
        }

        public void setCampaignPublicId(String campaignPublicId) {
            this.campaignPublicId = campaignPublicId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class CouponGenerateRequest extends CouponPayload {

        @NotBlank
        @Size(max = 36)
        private String campaignPublicId;

        @Size(max = 50)
        private String prefix = "CPN";

        @NotNull
        @Min(1)
        @Max(10_000)
        private Integer quantity;

        public String getCampaignPublicId() {
            return campaignPublicId;
        }

        public void setCampaignPublicId(String campaignPublicId) {
            this.campaignPublicId = campaignPublicId;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class CouponUpdateRequest {

        @Size(max = 255)
        private String name;
        private String description;
        private CouponType couponType;
        private CouponStatus status;
        private DistributionType distributionType;
        private Boolean stackable;
        private Boolean transferable;
        private Boolean reusable;
        private Boolean autoApply;
        @Min(0)
        private Integer priority;
        @Min(1)
        private Integer maxRedemptions;
        @Min(1)
        private Integer maxRedemptionsPerUser;
        private Instant validFrom;
        private Instant validTo;
        private JsonNode conditionsJson;
        private JsonNode actionsJson;
        private JsonNode metadataJson;

        @AssertTrue(message = "validTo must be after validFrom")
        public boolean isPeriodValid() {
            return validFrom == null || validTo == null || validTo.isAfter(validFrom);
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

        public CouponType getCouponType() {
            return couponType;
        }

        public void setCouponType(CouponType couponType) {
            this.couponType = couponType;
        }

        public CouponStatus getStatus() {
            return status;
        }

        public void setStatus(CouponStatus status) {
            this.status = status;
        }

        public DistributionType getDistributionType() {
            return distributionType;
        }

        public void setDistributionType(DistributionType distributionType) {
            this.distributionType = distributionType;
        }

        public Boolean getStackable() {
            return stackable;
        }

        public void setStackable(Boolean stackable) {
            this.stackable = stackable;
        }

        public Boolean getTransferable() {
            return transferable;
        }

        public void setTransferable(Boolean transferable) {
            this.transferable = transferable;
        }

        public Boolean getReusable() {
            return reusable;
        }

        public void setReusable(Boolean reusable) {
            this.reusable = reusable;
        }

        public Boolean getAutoApply() {
            return autoApply;
        }

        public void setAutoApply(Boolean autoApply) {
            this.autoApply = autoApply;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public Integer getMaxRedemptions() {
            return maxRedemptions;
        }

        public void setMaxRedemptions(Integer maxRedemptions) {
            this.maxRedemptions = maxRedemptions;
        }

        public Integer getMaxRedemptionsPerUser() {
            return maxRedemptionsPerUser;
        }

        public void setMaxRedemptionsPerUser(Integer maxRedemptionsPerUser) {
            this.maxRedemptionsPerUser = maxRedemptionsPerUser;
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

        public JsonNode getConditionsJson() {
            return conditionsJson;
        }

        public void setConditionsJson(JsonNode conditionsJson) {
            this.conditionsJson = conditionsJson;
        }

        public JsonNode getActionsJson() {
            return actionsJson;
        }

        public void setActionsJson(JsonNode actionsJson) {
            this.actionsJson = actionsJson;
        }

        public JsonNode getMetadataJson() {
            return metadataJson;
        }

        public void setMetadataJson(JsonNode metadataJson) {
            this.metadataJson = metadataJson;
        }
    }
}

package com.project.promotionservice.benefit.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class VoucherRequests {

    private static final String UUID_PATTERN =
            com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

    private VoucherRequests() {
    }

    public static class VoucherIssueRequest {

        @Size(max = 36)
        @Pattern(regexp = UUID_PATTERN, message = "campaignPublicId must be a valid UUID")
        private String campaignPublicId;

        @NotBlank
        @Size(max = 36)
        @Pattern(regexp = UUID_PATTERN, message = "ownerPublicId must be a valid UUID")
        private String ownerPublicId;

        @Size(max = 100)
        private String code;

        @NotBlank
        @Size(max = 255)
        private String name;

        private String description;

        @NotNull
        private VoucherType voucherType;

        @NotNull
        private VoucherSource source;

        @Size(max = 255)
        private String issueReason;

        @NotNull
        private Instant validFrom;

        @NotNull
        private Instant validTo;

        @NotNull
        private Boolean transferable = false;
        @NotNull
        private Boolean stackable = false;
        @NotNull
        private Boolean reusable = false;

        @NotNull
        @jakarta.validation.constraints.Min(1)
        private Integer maxUsage = 1;

        @DecimalMin("0.00")
        private BigDecimal faceValue;

        @DecimalMin("0.00")
        private BigDecimal minimumOrderAmount;

        @NotNull
        private JsonNode conditionsJson;

        @NotNull
        private JsonNode actionsJson;

        private JsonNode metadataJson;

        @AssertTrue(message = "validTo must be after validFrom")
        public boolean isPeriodValid() {
            return validFrom == null || validTo == null || validTo.isAfter(validFrom);
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

        public String getIssueReason() {
            return issueReason;
        }

        public void setIssueReason(String issueReason) {
            this.issueReason = issueReason;
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

    public static class VoucherBatchIssueRequest {

        @NotEmpty
        @Size(max = 1_000)
        private List<@Valid VoucherIssueRequest> vouchers;

        public List<VoucherIssueRequest> getVouchers() {
            return vouchers;
        }

        public void setVouchers(List<VoucherIssueRequest> vouchers) {
            this.vouchers = vouchers;
        }
    }

    public static class VoucherUpdateRequest {

        @Size(max = 255)
        private String name;
        private String description;
        private VoucherType voucherType;
        private VoucherStatus status;
        @Size(max = 255)
        private String issueReason;
        private Boolean transferable;
        private Boolean stackable;
        private Boolean reusable;
        @jakarta.validation.constraints.Min(1)
        private Integer maxUsage;
        @DecimalMin("0.00")
        private BigDecimal faceValue;
        @DecimalMin("0.00")
        private BigDecimal minimumOrderAmount;
        private JsonNode conditionsJson;
        private JsonNode actionsJson;
        private JsonNode metadataJson;

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

    public static class VoucherExtendRequest {

        @NotNull
        private Instant validTo;

        @NotBlank
        @Size(max = 255)
        private String reason;

        public Instant getValidTo() {
            return validTo;
        }

        public void setValidTo(Instant validTo) {
            this.validTo = validTo;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}

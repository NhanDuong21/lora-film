package com.project.promotionservice.benefit.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationType;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponType;
import com.project.promotionservice.benefit.enums.BenefitEnums.DistributionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class BenefitResponses {

    private BenefitResponses() {
    }

    public abstract static class AuditedResponse {
        private String publicId;
        private Instant createdAt;
        private String createdBy;
        private Instant updatedAt;
        private String updatedBy;

        public String getPublicId() {
            return publicId;
        }

        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }
    }

    public static class CouponResponse extends AuditedResponse {
        private String campaignPublicId;
        private String partnerPublicId;
        private String code;
        private String name;
        private String description;
        private CouponType couponType;
        private CouponStatus status;
        private DistributionType distributionType;
        private Boolean stackable;
        private Boolean transferable;
        private Boolean reusable;
        private Boolean autoApply;
        private Integer priority;
        private Integer maxRedemptions;
        private Integer redemptionCount;
        private Integer maxRedemptionsPerUser;
        private Instant validFrom;
        private Instant validTo;
        private JsonNode conditionsJson;
        private JsonNode actionsJson;
        private JsonNode metadataJson;

        public String getCampaignPublicId() {
            return campaignPublicId;
        }

        public void setCampaignPublicId(String campaignPublicId) {
            this.campaignPublicId = campaignPublicId;
        }

        public String getPartnerPublicId() {
            return partnerPublicId;
        }

        public void setPartnerPublicId(String partnerPublicId) {
            this.partnerPublicId = partnerPublicId;
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

        public Integer getRedemptionCount() {
            return redemptionCount;
        }

        public void setRedemptionCount(Integer redemptionCount) {
            this.redemptionCount = redemptionCount;
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

    public static class VoucherResponse extends AuditedResponse {
        private String campaignPublicId;
        private String partnerPublicId;
        private String ownerPublicId;
        private String code;
        private String name;
        private String description;
        private VoucherType voucherType;
        private VoucherSource source;
        private VoucherStatus status;
        private String issueReason;
        private String issuedBy;
        private Instant issuedAt;
        private Instant validFrom;
        private Instant validTo;
        private Boolean transferable;
        private Boolean stackable;
        private Boolean reusable;
        private Integer maxUsage;
        private Integer usageCount;
        private BigDecimal faceValue;
        private BigDecimal minimumOrderAmount;
        private JsonNode conditionsJson;
        private JsonNode actionsJson;
        private JsonNode metadataJson;

        public String getCampaignPublicId() {
            return campaignPublicId;
        }

        public void setCampaignPublicId(String campaignPublicId) {
            this.campaignPublicId = campaignPublicId;
        }

        public String getPartnerPublicId() {
            return partnerPublicId;
        }

        public void setPartnerPublicId(String partnerPublicId) {
            this.partnerPublicId = partnerPublicId;
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

    public static class ValidationResponse {
        private boolean valid;
        private RedemptionType benefitType;
        private String benefitPublicId;
        private String code;
        private BigDecimal originalAmount;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private String currency = "VND";
        private String reasonCode;
        private String message;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public RedemptionType getBenefitType() {
            return benefitType;
        }

        public void setBenefitType(RedemptionType benefitType) {
            this.benefitType = benefitType;
        }

        public String getBenefitPublicId() {
            return benefitPublicId;
        }

        public void setBenefitPublicId(String benefitPublicId) {
            this.benefitPublicId = benefitPublicId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        public void setOriginalAmount(BigDecimal originalAmount) {
            this.originalAmount = originalAmount;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
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

        public String getReasonCode() {
            return reasonCode;
        }

        public void setReasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class RedemptionResponse extends AuditedResponse {
        private RedemptionType redemptionType;
        private String benefitPublicId;
        private String campaignPublicId;
        private String code;
        private String reservationPublicId;
        private String bookingPublicId;
        private String orderPublicId;
        private String paymentPublicId;
        private String ownerPublicId;
        private String userPublicId;
        private RedemptionStatus status;
        private BigDecimal originalAmount;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private Instant confirmedAt;
        private Instant rollbackAt;
        private String rollbackReason;
        private Instant expiredAt;
        private JsonNode metadataJson;

        public RedemptionType getRedemptionType() {
            return redemptionType;
        }

        public void setRedemptionType(RedemptionType redemptionType) {
            this.redemptionType = redemptionType;
        }

        public String getBenefitPublicId() {
            return benefitPublicId;
        }

        public void setBenefitPublicId(String benefitPublicId) {
            this.benefitPublicId = benefitPublicId;
        }

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

        public String getReservationPublicId() {
            return reservationPublicId;
        }

        public void setReservationPublicId(String reservationPublicId) {
            this.reservationPublicId = reservationPublicId;
        }

        public String getBookingPublicId() {
            return bookingPublicId;
        }

        public void setBookingPublicId(String bookingPublicId) {
            this.bookingPublicId = bookingPublicId;
        }

        public String getOrderPublicId() {
            return orderPublicId;
        }

        public void setOrderPublicId(String orderPublicId) {
            this.orderPublicId = orderPublicId;
        }

        public String getPaymentPublicId() {
            return paymentPublicId;
        }

        public void setPaymentPublicId(String paymentPublicId) {
            this.paymentPublicId = paymentPublicId;
        }

        public String getOwnerPublicId() {
            return ownerPublicId;
        }

        public void setOwnerPublicId(String ownerPublicId) {
            this.ownerPublicId = ownerPublicId;
        }

        public String getUserPublicId() {
            return userPublicId;
        }

        public void setUserPublicId(String userPublicId) {
            this.userPublicId = userPublicId;
        }

        public RedemptionStatus getStatus() {
            return status;
        }

        public void setStatus(RedemptionStatus status) {
            this.status = status;
        }

        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        public void setOriginalAmount(BigDecimal originalAmount) {
            this.originalAmount = originalAmount;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
        }

        public BigDecimal getFinalAmount() {
            return finalAmount;
        }

        public void setFinalAmount(BigDecimal finalAmount) {
            this.finalAmount = finalAmount;
        }

        public Instant getConfirmedAt() {
            return confirmedAt;
        }

        public void setConfirmedAt(Instant confirmedAt) {
            this.confirmedAt = confirmedAt;
        }

        public Instant getRollbackAt() {
            return rollbackAt;
        }

        public void setRollbackAt(Instant rollbackAt) {
            this.rollbackAt = rollbackAt;
        }

        public String getRollbackReason() {
            return rollbackReason;
        }

        public void setRollbackReason(String rollbackReason) {
            this.rollbackReason = rollbackReason;
        }

        public Instant getExpiredAt() {
            return expiredAt;
        }

        public void setExpiredAt(Instant expiredAt) {
            this.expiredAt = expiredAt;
        }

        public JsonNode getMetadataJson() {
            return metadataJson;
        }

        public void setMetadataJson(JsonNode metadataJson) {
            this.metadataJson = metadataJson;
        }
    }

    public static class CompensationResponse extends AuditedResponse {
        private String voucherPublicId;
        private VoucherResponse voucher;
        private String reservationPublicId;
        private String bookingPublicId;
        private String orderPublicId;
        private String userPublicId;
        private CompensationType compensationType;
        private String reason;
        private BigDecimal amount;
        private CompensationStatus status;
        private Instant issuedAt;
        private Instant expiredAt;
        private JsonNode metadataJson;
        private List<ApprovalRecordResponse> approvalHistory = new ArrayList<>();

        public String getVoucherPublicId() {
            return voucherPublicId;
        }

        public void setVoucherPublicId(String voucherPublicId) {
            this.voucherPublicId = voucherPublicId;
        }

        public VoucherResponse getVoucher() {
            return voucher;
        }

        public void setVoucher(VoucherResponse voucher) {
            this.voucher = voucher;
        }

        public String getReservationPublicId() {
            return reservationPublicId;
        }

        public void setReservationPublicId(String reservationPublicId) {
            this.reservationPublicId = reservationPublicId;
        }

        public String getBookingPublicId() {
            return bookingPublicId;
        }

        public void setBookingPublicId(String bookingPublicId) {
            this.bookingPublicId = bookingPublicId;
        }

        public String getOrderPublicId() {
            return orderPublicId;
        }

        public void setOrderPublicId(String orderPublicId) {
            this.orderPublicId = orderPublicId;
        }

        public String getUserPublicId() {
            return userPublicId;
        }

        public void setUserPublicId(String userPublicId) {
            this.userPublicId = userPublicId;
        }

        public CompensationType getCompensationType() {
            return compensationType;
        }

        public void setCompensationType(CompensationType compensationType) {
            this.compensationType = compensationType;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public CompensationStatus getStatus() {
            return status;
        }

        public void setStatus(CompensationStatus status) {
            this.status = status;
        }

        public Instant getIssuedAt() {
            return issuedAt;
        }

        public void setIssuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
        }

        public Instant getExpiredAt() {
            return expiredAt;
        }

        public void setExpiredAt(Instant expiredAt) {
            this.expiredAt = expiredAt;
        }

        public JsonNode getMetadataJson() {
            return metadataJson;
        }

        public void setMetadataJson(JsonNode metadataJson) {
            this.metadataJson = metadataJson;
        }

        public List<ApprovalRecordResponse> getApprovalHistory() {
            return approvalHistory;
        }

        public void setApprovalHistory(List<ApprovalRecordResponse> approvalHistory) {
            this.approvalHistory = approvalHistory;
        }
    }

    public static class ApprovalRecordResponse extends AuditedResponse {
        private String action;
        private String oldStatus;
        private String newStatus;
        private String approverPublicId;
        private String comment;
        private Instant approvedAt;
        private JsonNode metadataJson;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getOldStatus() {
            return oldStatus;
        }

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public String getApproverPublicId() {
            return approverPublicId;
        }

        public void setApproverPublicId(String approverPublicId) {
            this.approverPublicId = approverPublicId;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Instant getApprovedAt() {
            return approvedAt;
        }

        public void setApprovedAt(Instant approvedAt) {
            this.approvedAt = approvedAt;
        }

        public JsonNode getMetadataJson() {
            return metadataJson;
        }

        public void setMetadataJson(JsonNode metadataJson) {
            this.metadataJson = metadataJson;
        }
    }

    public static class CouponImportResult {
        private int totalRows;
        private int importedRows;
        private int rejectedRows;
        private List<String> errors = new ArrayList<>();
        private List<CouponResponse> coupons = new ArrayList<>();

        public int getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        public int getImportedRows() {
            return importedRows;
        }

        public void setImportedRows(int importedRows) {
            this.importedRows = importedRows;
        }

        public int getRejectedRows() {
            return rejectedRows;
        }

        public void setRejectedRows(int rejectedRows) {
            this.rejectedRows = rejectedRows;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public List<CouponResponse> getCoupons() {
            return coupons;
        }

        public void setCoupons(List<CouponResponse> coupons) {
            this.coupons = coupons;
        }
    }
}

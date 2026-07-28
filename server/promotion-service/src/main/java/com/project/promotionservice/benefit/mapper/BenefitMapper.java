package com.project.promotionservice.benefit.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponCreateRequest;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponGenerateRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherIssueRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CompensationResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.ApprovalRecordResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CouponResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.RedemptionResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.VoucherResponse;
import com.project.promotionservice.benefit.entity.BenefitAuditableRecord;
import com.project.promotionservice.benefit.entity.CompensationVoucher;
import com.project.promotionservice.benefit.entity.CompensationApprovalHistory;
import com.project.promotionservice.benefit.entity.Coupon;
import com.project.promotionservice.benefit.entity.CouponRedemption;
import com.project.promotionservice.benefit.entity.Voucher;
import com.project.promotionservice.benefit.entity.VoucherRedemption;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.common.entity.BaseAuditableEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BenefitMapper {

    private final ObjectMapper objectMapper;

    public BenefitMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Coupon toCoupon(CouponCreateRequest request, String actor) {
        Coupon coupon = new Coupon();
        coupon.setCampaignPublicId(request.getCampaignPublicId());
        coupon.setCode(normalizeCode(request.getCode()));
        applyCouponPayload(coupon, request, actor);
        return coupon;
    }

    public Coupon toGeneratedCoupon(CouponGenerateRequest request, String code, String actor) {
        Coupon coupon = new Coupon();
        coupon.setCampaignPublicId(request.getCampaignPublicId());
        coupon.setCode(normalizeCode(code));
        coupon.setName(request.getName());
        coupon.setDescription(request.getDescription());
        coupon.setCouponType(request.getCouponType());
        coupon.setStatus(request.getStatus() == null ? CouponStatus.ACTIVE : request.getStatus());
        coupon.setDistributionType(request.getDistributionType());
        coupon.setStackable(defaultFalse(request.getStackable()));
        coupon.setTransferable(defaultFalse(request.getTransferable()));
        coupon.setReusable(defaultFalse(request.getReusable()));
        coupon.setAutoApply(defaultFalse(request.getAutoApply()));
        coupon.setPriority(request.getPriority() == null ? 100 : request.getPriority());
        coupon.setMaxRedemptions(request.getMaxRedemptions());
        coupon.setMaxRedemptionsPerUser(request.getMaxRedemptionsPerUser());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());
        coupon.setConditionsJson(toJson(request.getConditionsJson()));
        coupon.setActionsJson(toJson(request.getActionsJson()));
        coupon.setMetadataJson(toNullableJson(request.getMetadataJson()));
        coupon.setCreatedBy(actor);
        coupon.setUpdatedBy(actor);
        return coupon;
    }

    private void applyCouponPayload(Coupon coupon, CouponCreateRequest request, String actor) {
        coupon.setName(request.getName());
        coupon.setDescription(request.getDescription());
        coupon.setCouponType(request.getCouponType());
        coupon.setStatus(request.getStatus() == null ? CouponStatus.ACTIVE : request.getStatus());
        coupon.setDistributionType(request.getDistributionType());
        coupon.setStackable(defaultFalse(request.getStackable()));
        coupon.setTransferable(defaultFalse(request.getTransferable()));
        coupon.setReusable(defaultFalse(request.getReusable()));
        coupon.setAutoApply(defaultFalse(request.getAutoApply()));
        coupon.setPriority(request.getPriority() == null ? 100 : request.getPriority());
        coupon.setMaxRedemptions(request.getMaxRedemptions());
        coupon.setMaxRedemptionsPerUser(request.getMaxRedemptionsPerUser());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());
        coupon.setConditionsJson(toJson(request.getConditionsJson()));
        coupon.setActionsJson(toJson(request.getActionsJson()));
        coupon.setMetadataJson(toNullableJson(request.getMetadataJson()));
        coupon.setCreatedBy(actor);
        coupon.setUpdatedBy(actor);
    }

    public Voucher toVoucher(VoucherIssueRequest request, String code, String actor) {
        Voucher voucher = new Voucher();
        voucher.setCampaignPublicId(request.getCampaignPublicId());
        voucher.setOwnerPublicId(request.getOwnerPublicId());
        voucher.setCode(normalizeCode(code));
        voucher.setName(request.getName());
        voucher.setDescription(request.getDescription());
        voucher.setVoucherType(request.getVoucherType());
        voucher.setSource(request.getSource());
        voucher.setStatus(request.getValidFrom().isAfter(Instant.now()) ? VoucherStatus.ISSUED : VoucherStatus.ACTIVE);
        voucher.setIssueReason(request.getIssueReason());
        voucher.setIssuedBy(actor);
        voucher.setIssuedAt(Instant.now());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidTo(request.getValidTo());
        voucher.setTransferable(defaultFalse(request.getTransferable()));
        voucher.setStackable(defaultFalse(request.getStackable()));
        voucher.setReusable(defaultFalse(request.getReusable()));
        voucher.setMaxUsage(request.getMaxUsage());
        voucher.setFaceValue(request.getFaceValue());
        voucher.setMinimumOrderAmount(request.getMinimumOrderAmount());
        voucher.setConditionsJson(toJson(request.getConditionsJson()));
        voucher.setActionsJson(toJson(request.getActionsJson()));
        voucher.setMetadataJson(toNullableJson(request.getMetadataJson()));
        voucher.setCreatedBy(actor);
        voucher.setUpdatedBy(actor);
        return voucher;
    }

    public CouponResponse toCouponResponse(Coupon entity) {
        CouponResponse response = new CouponResponse();
        mapAudited(entity, response);
        response.setCampaignPublicId(entity.getCampaignPublicId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setCouponType(entity.getCouponType());
        response.setStatus(entity.getStatus());
        response.setDistributionType(entity.getDistributionType());
        response.setStackable(entity.getStackable());
        response.setTransferable(entity.getTransferable());
        response.setReusable(entity.getReusable());
        response.setAutoApply(entity.getAutoApply());
        response.setPriority(entity.getPriority());
        response.setMaxRedemptions(entity.getMaxRedemptions());
        response.setRedemptionCount(entity.getRedemptionCount());
        response.setMaxRedemptionsPerUser(entity.getMaxRedemptionsPerUser());
        response.setValidFrom(entity.getValidFrom());
        response.setValidTo(entity.getValidTo());
        response.setConditionsJson(toNode(entity.getConditionsJson()));
        response.setActionsJson(toNode(entity.getActionsJson()));
        response.setMetadataJson(toNode(entity.getMetadataJson()));
        return response;
    }

    public VoucherResponse toVoucherResponse(Voucher entity) {
        VoucherResponse response = new VoucherResponse();
        mapAudited(entity, response);
        response.setCampaignPublicId(entity.getCampaignPublicId());
        response.setOwnerPublicId(entity.getOwnerPublicId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setVoucherType(entity.getVoucherType());
        response.setSource(entity.getSource());
        response.setStatus(entity.getStatus());
        response.setIssueReason(entity.getIssueReason());
        response.setIssuedBy(entity.getIssuedBy());
        response.setIssuedAt(entity.getIssuedAt());
        response.setValidFrom(entity.getValidFrom());
        response.setValidTo(entity.getValidTo());
        response.setTransferable(entity.getTransferable());
        response.setStackable(entity.getStackable());
        response.setReusable(entity.getReusable());
        response.setMaxUsage(entity.getMaxUsage());
        response.setUsageCount(entity.getUsageCount());
        response.setFaceValue(entity.getFaceValue());
        response.setMinimumOrderAmount(entity.getMinimumOrderAmount());
        response.setConditionsJson(toNode(entity.getConditionsJson()));
        response.setActionsJson(toNode(entity.getActionsJson()));
        response.setMetadataJson(toNode(entity.getMetadataJson()));
        return response;
    }

    public RedemptionResponse toRedemptionResponse(CouponRedemption entity) {
        RedemptionResponse response = new RedemptionResponse();
        mapAudited(entity, response);
        response.setRedemptionType(RedemptionType.COUPON);
        response.setBenefitPublicId(entity.getCouponPublicId());
        response.setCampaignPublicId(entity.getCampaignPublicId());
        response.setCode(entity.getRedeemedCode());
        response.setReservationPublicId(entity.getReservationPublicId());
        response.setBookingPublicId(entity.getBookingPublicId());
        response.setOrderPublicId(entity.getOrderPublicId());
        response.setPaymentPublicId(entity.getPaymentPublicId());
        response.setUserPublicId(entity.getUserPublicId());
        response.setStatus(entity.getStatus());
        response.setOriginalAmount(entity.getOriginalAmount());
        response.setDiscountAmount(entity.getDiscountAmount());
        response.setFinalAmount(entity.getFinalAmount());
        response.setConfirmedAt(entity.getConfirmedAt());
        response.setRollbackAt(entity.getRollbackAt());
        response.setRollbackReason(entity.getRollbackReason());
        response.setExpiredAt(entity.getExpiredAt());
        response.setMetadataJson(toNode(entity.getMetadataJson()));
        return response;
    }

    public RedemptionResponse toRedemptionResponse(VoucherRedemption entity) {
        RedemptionResponse response = new RedemptionResponse();
        mapAudited(entity, response);
        response.setRedemptionType(RedemptionType.VOUCHER);
        response.setBenefitPublicId(entity.getVoucherPublicId());
        response.setCampaignPublicId(entity.getCampaignPublicId());
        response.setReservationPublicId(entity.getReservationPublicId());
        response.setBookingPublicId(entity.getBookingPublicId());
        response.setOrderPublicId(entity.getOrderPublicId());
        response.setPaymentPublicId(entity.getPaymentPublicId());
        response.setOwnerPublicId(entity.getOwnerPublicId());
        response.setUserPublicId(entity.getRedeemedBy());
        response.setStatus(entity.getStatus());
        response.setOriginalAmount(entity.getOriginalAmount());
        response.setDiscountAmount(entity.getDiscountAmount());
        response.setFinalAmount(entity.getFinalAmount());
        response.setConfirmedAt(entity.getConfirmedAt());
        response.setRollbackAt(entity.getRollbackAt());
        response.setRollbackReason(entity.getRollbackReason());
        response.setExpiredAt(entity.getExpiredAt());
        response.setMetadataJson(toNode(entity.getMetadataJson()));
        return response;
    }

    public CompensationResponse toCompensationResponse(CompensationVoucher entity, Voucher voucher) {
        CompensationResponse response = new CompensationResponse();
        mapAudited(entity, response);
        response.setVoucherPublicId(entity.getVoucherPublicId());
        response.setVoucher(voucher == null ? null : toVoucherResponse(voucher));
        response.setReservationPublicId(entity.getReservationPublicId());
        response.setBookingPublicId(entity.getBookingPublicId());
        response.setOrderPublicId(entity.getOrderPublicId());
        response.setUserPublicId(entity.getUserPublicId());
        response.setCompensationType(entity.getCompensationType());
        response.setReason(entity.getReason());
        response.setAmount(entity.getAmount());
        response.setStatus(entity.getStatus());
        response.setIssuedAt(entity.getIssuedAt());
        response.setExpiredAt(entity.getExpiredAt());
        response.setMetadataJson(toNode(entity.getMetadataJson()));
        return response;
    }

    public ApprovalRecordResponse toApprovalResponse(CompensationApprovalHistory entity) {
        ApprovalRecordResponse response = new ApprovalRecordResponse();
        mapAudited(entity, response);
        response.setAction(entity.getAction());
        response.setOldStatus(entity.getOldStatus());
        response.setNewStatus(entity.getNewStatus());
        response.setApproverPublicId(entity.getApproverPublicId());
        response.setComment(entity.getComment());
        response.setApprovedAt(entity.getApprovedAt());
        response.setMetadataJson(toNode(entity.getMetadataJson()));
        return response;
    }

    public String toJson(JsonNode node) {
        return node == null ? "{}" : node.toString();
    }

    public String toNullableJson(JsonNode node) {
        return node == null || node.isNull() ? null : node.toString();
    }

    public JsonNode toNode(String json) {
        if (json == null || json.isBlank()) {
            return NullNode.getInstance();
        }
        try {
            JsonNode parsed = objectMapper.readTree(json);
            if (parsed != null && parsed.isTextual()) {
                parsed = objectMapper.readTree(parsed.asText());
            }
            return parsed;
        } catch (JsonProcessingException exception) {
            return objectMapper.getNodeFactory().textNode(json);
        }
    }

    public String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private boolean defaultFalse(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private void mapAudited(BaseAuditableEntity entity,
                            com.project.promotionservice.benefit.dto.response.BenefitResponses.AuditedResponse response) {
        response.setPublicId(entity.getPublicId());
        response.setCreatedAt(entity.getCreatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setUpdatedBy(entity.getUpdatedBy());
    }

    private void mapAudited(BenefitAuditableRecord entity,
                            com.project.promotionservice.benefit.dto.response.BenefitResponses.AuditedResponse response) {
        response.setPublicId(entity.getPublicId());
        response.setCreatedAt(entity.getCreatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setUpdatedBy(entity.getUpdatedBy());
    }
}

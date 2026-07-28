package com.project.promotionservice.benefit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.benefit.dto.request.RedemptionRequests.BenefitRedeemRequest;
import com.project.promotionservice.benefit.dto.request.RedemptionRequests.BenefitValidationRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.RedemptionResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.ValidationResponse;
import com.project.promotionservice.benefit.entity.Coupon;
import com.project.promotionservice.benefit.entity.CouponRedemption;
import com.project.promotionservice.benefit.entity.Voucher;
import com.project.promotionservice.benefit.entity.VoucherRedemption;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponType;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherType;
import com.project.promotionservice.benefit.exception.BenefitErrorCode;
import com.project.promotionservice.benefit.mapper.BenefitMapper;
import com.project.promotionservice.benefit.repository.BenefitCampaignRepository;
import com.project.promotionservice.benefit.repository.CouponRedemptionRepository;
import com.project.promotionservice.benefit.repository.CouponRepository;
import com.project.promotionservice.benefit.repository.VoucherRedemptionRepository;
import com.project.promotionservice.benefit.repository.VoucherRepository;
import com.project.promotionservice.benefit.service.BenefitEventService;
import com.project.promotionservice.benefit.service.RedemptionService;
import com.project.promotionservice.benefit.specification.BenefitSpecifications;
import com.project.promotionservice.common.audit.Auditable;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RedemptionServiceImpl implements RedemptionService {

    private static final Set<RedemptionStatus> CONSUMED_STATUSES =
            EnumSet.of(RedemptionStatus.SUCCESS, RedemptionStatus.CONFIRMED);

    private final CouponRepository couponRepository;
    private final VoucherRepository voucherRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final BenefitCampaignRepository campaignRepository;
    private final BenefitMapper mapper;
    private final BenefitEventService eventService;
    private final ObjectMapper objectMapper;

    public RedemptionServiceImpl(CouponRepository couponRepository,
                                 VoucherRepository voucherRepository,
                                 CouponRedemptionRepository couponRedemptionRepository,
                                 VoucherRedemptionRepository voucherRedemptionRepository,
                                 BenefitCampaignRepository campaignRepository,
                                 BenefitMapper mapper,
                                 BenefitEventService eventService,
                                 ObjectMapper objectMapper) {
        this.couponRepository = couponRepository;
        this.voucherRepository = voucherRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
        this.voucherRedemptionRepository = voucherRedemptionRepository;
        this.campaignRepository = campaignRepository;
        this.mapper = mapper;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResponse validateCoupon(BenefitValidationRequest request) {
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(request.getCode())
                .orElseThrow(() -> notFound(BenefitErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
        return validateCouponEntity(coupon, request);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResponse validateVoucher(BenefitValidationRequest request) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(request.getCode())
                .orElseThrow(() -> notFound(BenefitErrorCode.VOUCHER_NOT_FOUND, "Voucher not found"));
        return validateVoucherEntity(voucher, request);
    }

    @Override
    @Transactional
    @Auditable(action = "CONFIRM_RESERVED", entityType = "COUPON_REDEMPTION")
    @Caching(evict = {
            @CacheEvict(cacheNames = "promotions", allEntries = true),
            @CacheEvict(cacheNames = "vouchers", allEntries = true)
    })
    public RedemptionResponse confirmReservedCoupon(BenefitRedeemRequest request, String actor) {
        requireReservationReference(request);
        return confirmCouponRedemption(request, actor);
    }

    private RedemptionResponse confirmCouponRedemption(
            BenefitRedeemRequest request, String actor) {
        Coupon coupon = couponRepository.findByCodeForUpdate(request.getCode())
                .orElseThrow(() -> notFound(BenefitErrorCode.COUPON_NOT_FOUND, "Coupon not found"));

        var reservedRedemption = couponRedemptionRepository
                .findByReservationPublicIdAndDeletedAtIsNull(request.getReservationPublicId());
        if (reservedRedemption.isPresent()) {
            return mapper.toRedemptionResponse(reservedRedemption.get());
        }
        var existing = couponRedemptionRepository.findProcessedTransaction(
                coupon.getPublicId(), request.getUserPublicId(), request.getOrderPublicId(),
                request.getBookingPublicId(), request.getPaymentPublicId(), CONSUMED_STATUSES);
        if (existing.isPresent()) {
            requireSameReservation(existing.get().getReservationPublicId(),
                    request.getReservationPublicId());
            return mapper.toRedemptionResponse(existing.get());
        }

        PromotionCampaign campaign = requireActiveCampaignForUpdate(coupon.getCampaignPublicId());
        ValidationResponse validation = validateCouponEntity(coupon, request);
        consumeCampaignCapacity(campaign, validation.getDiscountAmount());

        CouponRedemption redemption = new CouponRedemption();
        redemption.setCouponPublicId(coupon.getPublicId());
        redemption.setCampaignPublicId(coupon.getCampaignPublicId());
        redemption.setReservationPublicId(request.getReservationPublicId());
        redemption.setBookingPublicId(request.getBookingPublicId());
        redemption.setOrderPublicId(request.getOrderPublicId());
        redemption.setPaymentPublicId(request.getPaymentPublicId());
        redemption.setUserPublicId(request.getUserPublicId());
        redemption.setCustomerPhone(request.getCustomerPhone());
        redemption.setRedeemedCode(coupon.getCode());
        redemption.setStatus(RedemptionStatus.CONFIRMED);
        redemption.setDiscountAmount(validation.getDiscountAmount());
        redemption.setOriginalAmount(validation.getOriginalAmount());
        redemption.setFinalAmount(validation.getFinalAmount());
        redemption.setConfirmedAt(Instant.now());
        redemption.setMetadataJson(mapper.toNullableJson(request.getContextJson()));
        redemption.setCreatedBy(actor);
        redemption.setUpdatedBy(actor);

        coupon.setRedemptionCount(coupon.getRedemptionCount() + 1);
        if (coupon.getCouponType() == CouponType.SINGLE_USE
                || (coupon.getMaxRedemptions() != null
                && coupon.getRedemptionCount() >= coupon.getMaxRedemptions())) {
            coupon.setStatus(CouponStatus.USED);
        }
        coupon.setUpdatedBy(actor);
        couponRepository.save(coupon);
        campaignRepository.save(campaign);

        CouponRedemption saved = couponRedemptionRepository.save(redemption);
        RedemptionResponse response = mapper.toRedemptionResponse(saved);
        eventService.record("COUPON", coupon.getPublicId(), "COUPON_REDEEMED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "CONFIRM_RESERVED", entityType = "VOUCHER_REDEMPTION")
    @Caching(evict = {
            @CacheEvict(cacheNames = "promotions", allEntries = true),
            @CacheEvict(cacheNames = "vouchers", allEntries = true)
    })
    public RedemptionResponse confirmReservedVoucher(BenefitRedeemRequest request, String actor) {
        requireReservationReference(request);
        return confirmVoucherRedemption(request, actor);
    }

    private RedemptionResponse confirmVoucherRedemption(
            BenefitRedeemRequest request, String actor) {
        Voucher voucher = voucherRepository.findByCodeForUpdate(request.getCode())
                .orElseThrow(() -> notFound(BenefitErrorCode.VOUCHER_NOT_FOUND, "Voucher not found"));

        var reservedRedemption = voucherRedemptionRepository
                .findByReservationPublicIdAndDeletedAtIsNull(request.getReservationPublicId());
        if (reservedRedemption.isPresent()) {
            RedemptionResponse response = mapper.toRedemptionResponse(reservedRedemption.get());
            response.setCode(voucher.getCode());
            return response;
        }
        var existing = voucherRedemptionRepository.findProcessedTransaction(
                voucher.getPublicId(), request.getUserPublicId(), request.getOrderPublicId(),
                request.getBookingPublicId(), request.getPaymentPublicId(), CONSUMED_STATUSES);
        if (existing.isPresent()) {
            requireSameReservation(existing.get().getReservationPublicId(),
                    request.getReservationPublicId());
            RedemptionResponse response = mapper.toRedemptionResponse(existing.get());
            response.setCode(voucher.getCode());
            return response;
        }
        boolean allowMultipleVoucherPerOrder = Boolean.TRUE.equals(voucher.getStackable())
                && readJson(voucher.getConditionsJson())
                .path("allowMultipleVoucherPerOrder").asBoolean(false);
        if (!allowMultipleVoucherPerOrder
                && request.getOrderPublicId() != null
                && voucherRedemptionRepository.existsByOrderPublicIdAndStatusIn(
                        request.getOrderPublicId(), CONSUMED_STATUSES)) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Only one voucher can be applied to an order");
        }

        PromotionCampaign campaign = null;
        if (voucher.getCampaignPublicId() != null) {
            campaign = requireActiveCampaignForUpdate(voucher.getCampaignPublicId());
        }
        ValidationResponse validation = validateVoucherEntity(voucher, request);
        if (campaign != null) {
            consumeCampaignCapacity(campaign, validation.getDiscountAmount());
        }

        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucherPublicId(voucher.getPublicId());
        redemption.setCampaignPublicId(voucher.getCampaignPublicId());
        redemption.setReservationPublicId(request.getReservationPublicId());
        redemption.setBookingPublicId(request.getBookingPublicId());
        redemption.setOrderPublicId(request.getOrderPublicId());
        redemption.setPaymentPublicId(request.getPaymentPublicId());
        redemption.setOwnerPublicId(voucher.getOwnerPublicId());
        redemption.setRedeemedBy(request.getUserPublicId());
        redemption.setStatus(RedemptionStatus.CONFIRMED);
        redemption.setOriginalAmount(validation.getOriginalAmount());
        redemption.setDiscountAmount(validation.getDiscountAmount());
        redemption.setFinalAmount(validation.getFinalAmount());
        redemption.setConfirmedAt(Instant.now());
        redemption.setMetadataJson(mapper.toNullableJson(request.getContextJson()));
        redemption.setCreatedBy(actor);
        redemption.setUpdatedBy(actor);

        voucher.setUsageCount(voucher.getUsageCount() + 1);
        if (voucher.getUsageCount() >= voucher.getMaxUsage()) {
            voucher.setStatus(VoucherStatus.USED);
        }
        voucher.setUpdatedBy(actor);
        voucherRepository.save(voucher);
        if (campaign != null) {
            campaignRepository.save(campaign);
        }

        VoucherRedemption saved = voucherRedemptionRepository.save(redemption);
        RedemptionResponse response = mapper.toRedemptionResponse(saved);
        response.setCode(voucher.getCode());
        eventService.record("VOUCHER", voucher.getPublicId(), "VOUCHER_REDEEMED", response, actor);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RedemptionResponse> history(
            RedemptionType type, String userPublicId, String bookingPublicId,
            RedemptionStatus status, Instant from, Instant to, int page, int size) {
        List<RedemptionResponse> history = new ArrayList<>();
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (type == null || type == RedemptionType.COUPON) {
            couponRedemptionRepository.findAll(
                            BenefitSpecifications.couponRedemptions(
                                    userPublicId, bookingPublicId, status, from, to), sort)
                    .stream().map(mapper::toRedemptionResponse).forEach(history::add);
        }
        if (type == null || type == RedemptionType.VOUCHER) {
            voucherRedemptionRepository.findAll(
                            BenefitSpecifications.voucherRedemptions(
                                    userPublicId, bookingPublicId, status, from, to), sort)
                    .stream().map(mapper::toRedemptionResponse).forEach(history::add);
        }
        history.sort(Comparator.comparing(
                RedemptionResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        int fromIndex = Math.min(safePage * safeSize, history.size());
        int toIndex = Math.min(fromIndex + safeSize, history.size());
        int totalPages = history.isEmpty() ? 0 : (int) Math.ceil((double) history.size() / safeSize);
        return new PagedResponse<>(
                new ArrayList<>(history.subList(fromIndex, toIndex)),
                safePage, safeSize, history.size(), totalPages, safePage >= Math.max(0, totalPages - 1));
    }

    private ValidationResponse validateCouponEntity(Coupon coupon, BenefitValidationRequest request) {
        Instant now = Instant.now();
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw badRequest(BenefitErrorCode.COUPON_INACTIVE, "Coupon is not active");
        }
        if (now.isBefore(coupon.getValidFrom()) || !now.isBefore(coupon.getValidTo())) {
            throw badRequest(BenefitErrorCode.COUPON_EXPIRED, "Coupon is outside its validity period");
        }
        PromotionCampaign campaign = requireActiveCampaign(coupon.getCampaignPublicId());
        requireCampaignUserCapacity(campaign, request.getUserPublicId());
        if (coupon.getMaxRedemptions() != null
                && coupon.getRedemptionCount() >= coupon.getMaxRedemptions()) {
            throw badRequest(BenefitErrorCode.COUPON_EXHAUSTED, "Coupon usage limit has been reached");
        }
        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw badRequest(
                    BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Verified customerPhone is required to use a coupon");
        }
        long userUsage = couponRedemptionRepository.countByCouponPublicIdAndUserPublicIdAndStatusIn(
                coupon.getPublicId(), request.getUserPublicId(), CONSUMED_STATUSES);
        if (userUsage >= coupon.getMaxRedemptionsPerUser()) {
            throw badRequest(BenefitErrorCode.COUPON_USER_LIMIT_REACHED,
                    "Coupon usage limit for this user has been reached");
        }
        long phoneUsage = couponRedemptionRepository.countByCouponPublicIdAndCustomerPhoneAndStatusIn(
                coupon.getPublicId(), request.getCustomerPhone(), CONSUMED_STATUSES);
        if (phoneUsage >= coupon.getMaxRedemptionsPerUser()) {
            throw badRequest(BenefitErrorCode.COUPON_USER_LIMIT_REACHED,
                    "Coupon usage limit for this verified phone has been reached");
        }
        JsonNode conditions = readJson(coupon.getConditionsJson());
        validateConditions(conditions, request);
        BigDecimal discount = calculateDiscount(
                readJson(coupon.getActionsJson()), null, request.getOriginalAmount());
        requireCampaignCapacity(campaign, discount);
        return validResponse(
                RedemptionType.COUPON, coupon.getPublicId(), coupon.getCode(),
                request.getOriginalAmount(), discount);
    }

    private ValidationResponse validateVoucherEntity(Voucher voucher, BenefitValidationRequest request) {
        Instant now = Instant.now();
        if (voucher.getStatus() == VoucherStatus.USED) {
            throw badRequest(BenefitErrorCode.VOUCHER_ALREADY_USED, "Voucher has already been used");
        }
        if (voucher.getStatus() != VoucherStatus.ACTIVE && voucher.getStatus() != VoucherStatus.ISSUED) {
            throw badRequest(BenefitErrorCode.VOUCHER_INVALID, "Voucher is not available");
        }
        if (now.isBefore(voucher.getValidFrom()) || !now.isBefore(voucher.getValidTo())) {
            throw badRequest(BenefitErrorCode.VOUCHER_EXPIRED, "Voucher is outside its validity period");
        }
        if (!voucher.getOwnerPublicId().equals(request.getUserPublicId())
                && !Boolean.TRUE.equals(voucher.getTransferable())) {
            throw new BusinessException(
                    BenefitErrorCode.VOUCHER_OWNER_MISMATCH,
                    "Voucher belongs to another customer",
                    HttpStatus.FORBIDDEN);
        }
        if (voucher.getUsageCount() >= voucher.getMaxUsage()) {
            throw badRequest(BenefitErrorCode.VOUCHER_ALREADY_USED, "Voucher usage limit has been reached");
        }
        if (voucher.getMinimumOrderAmount() != null
                && request.getOriginalAmount().compareTo(voucher.getMinimumOrderAmount()) < 0) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Minimum order amount is not met");
        }
        PromotionCampaign campaign = null;
        if (voucher.getCampaignPublicId() != null) {
            campaign = requireActiveCampaign(voucher.getCampaignPublicId());
            requireCampaignUserCapacity(campaign, request.getUserPublicId());
        }
        validateConditions(readJson(voucher.getConditionsJson()), request);
        validateVoucherPolicy(voucher, request);
        BigDecimal discount = calculateDiscount(
                readJson(voucher.getActionsJson()), voucher, request.getOriginalAmount());
        if (campaign != null) {
            requireCampaignCapacity(campaign, discount);
        }
        return validResponse(
                RedemptionType.VOUCHER, voucher.getPublicId(), voucher.getCode(),
                request.getOriginalAmount(), discount);
    }

    private void validateVoucherPolicy(Voucher voucher, BenefitValidationRequest request) {
        JsonNode context = request.getContextJson();
        if (voucher.getVoucherType() == VoucherType.FREE_TICKET
                && (voucher.getSource() == VoucherSource.BIRTHDAY
                || voucher.getSource() == VoucherSource.TIER_UPGRADE)) {
            if (context == null || !context.isObject()) {
                throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                        "Showtime context is required for free-ticket voucher");
            }
            String format = context.path("format").asText("");
            boolean specialFormat = Set.of("IMAX", "4DX", "SCREENX", "GOLD_CLASS")
                    .contains(format.toUpperCase(Locale.ROOT));
            if (specialFormat
                    || context.path("isPremiere").asBoolean(false)
                    || context.path("isEarlyScreening").asBoolean(false)
                    || context.path("isHoliday").asBoolean(false)) {
                throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                        "Free-ticket voucher is not eligible for this showtime");
            }
        }
        String orderType = context == null ? "" : context.path("orderType").asText("");
        if ("GROUP_BOOKING".equalsIgnoreCase(orderType)
                && (voucher.getVoucherType() == VoucherType.MEMBERSHIP
                || voucher.getSource() == VoucherSource.BIRTHDAY
                || voucher.getSource() == VoucherSource.TIER_UPGRADE)) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Membership voucher cannot be used for group booking");
        }
    }

    private void validateConditions(JsonNode conditions, BenefitValidationRequest request) {
        if (conditions == null || !conditions.isObject()) return;
        BigDecimal minimum = decimal(conditions, "minimumOrderAmount", "minOrderAmount");
        if (minimum != null && request.getOriginalAmount().compareTo(minimum) < 0) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Minimum order amount is not met");
        }
        JsonNode context = request.getContextJson();
        matchAllowed(conditions, context, "movieIds", "movieId");
        matchAllowed(conditions, context, "cinemaIds", "cinemaId");
        matchAllowed(conditions, context, "paymentMethods", "paymentMethod");
        matchAllowed(conditions, context, "channels", "channel");
        matchAllowed(conditions, context, "formats", "format");
        matchAllowed(conditions, context, "orderTypes", "orderType");
        JsonNode allowedUsers = conditions.get("allowedUserIds");
        if (allowedUsers != null && allowedUsers.isArray() && !arrayContains(allowedUsers, request.getUserPublicId())) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Customer is not eligible for this benefit");
        }
    }

    private void matchAllowed(JsonNode conditions, JsonNode context, String conditionField, String contextField) {
        JsonNode allowed = conditions.get(conditionField);
        if (allowed == null || !allowed.isArray() || allowed.isEmpty()) return;
        String actual = context == null || context.get(contextField) == null
                ? null : context.get(contextField).asText();
        if (actual == null || !arrayContains(allowed, actual)) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Condition not met: " + contextField);
        }
    }

    private boolean arrayContains(JsonNode values, String expected) {
        for (JsonNode value : values) {
            if (value.asText().equalsIgnoreCase(expected)) return true;
        }
        return false;
    }

    private BigDecimal calculateDiscount(JsonNode actions, Voucher voucher, BigDecimal originalAmount) {
        JsonNode action = actions;
        if (actions != null && actions.isArray() && !actions.isEmpty()) {
            action = actions.get(0);
        }
        if (action == null || !action.isObject()) {
            if (voucher != null && voucher.getFaceValue() != null) {
                return money(voucher.getFaceValue().min(originalAmount));
            }
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Benefit action is not configured");
        }
        String type = text(action, "discountType", "type", "actionType");
        if (type == null && voucher != null) {
            type = voucher.getVoucherType().name();
        }
        BigDecimal value = decimal(action, "discountValue", "value", "amount", "percentage");
        BigDecimal discount;
        String normalizedType = type == null ? "" : type.toUpperCase(Locale.ROOT);
        if (normalizedType.contains("PERCENT")) {
            if (value == null) {
                throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                        "Percentage value is missing");
            }
            if (value.signum() <= 0 || value.compareTo(BigDecimal.valueOf(50)) > 0) {
                throw badRequest(
                        BenefitErrorCode.BENEFIT_CONFIGURATION_INVALID,
                        "Percentage discount must be greater than zero and cannot exceed 50%");
            }
            discount = originalAmount.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal maximum = decimal(action, "maxDiscountAmount", "maximumDiscountAmount", "maxAmount");
            if (maximum != null) discount = discount.min(maximum);
        } else if (normalizedType.equals(VoucherType.FREE_TICKET.name())
                || normalizedType.equals("FREE")
                || normalizedType.equals("FULL_DISCOUNT")) {
            discount = originalAmount;
        } else if (normalizedType.contains("FIXED") || normalizedType.contains("AMOUNT")
                || normalizedType.equals("CASHBACK")) {
            if (value == null && voucher != null) {
                value = voucher.getFaceValue();
            }
            if (value == null) {
                throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                        "Fixed discount value is missing");
            }
            discount = value;
        } else if (value != null) {
            discount = value;
        } else {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Unsupported benefit action type");
        }
        if (discount.signum() < 0) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Discount amount cannot be negative");
        }
        return money(discount.min(originalAmount));
    }

    private ValidationResponse validResponse(
            RedemptionType type, String publicId, String code,
            BigDecimal originalAmount, BigDecimal discountAmount) {
        ValidationResponse response = new ValidationResponse();
        response.setValid(true);
        response.setBenefitType(type);
        response.setBenefitPublicId(publicId);
        response.setCode(code);
        response.setOriginalAmount(money(originalAmount));
        response.setDiscountAmount(money(discountAmount));
        response.setFinalAmount(money(originalAmount.subtract(discountAmount).max(BigDecimal.ZERO)));
        response.setReasonCode("ELIGIBLE");
        response.setMessage("Benefit is eligible");
        return response;
    }

    private PromotionCampaign requireActiveCampaign(String publicId) {
        PromotionCampaign campaign = campaignRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound(BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                        "Promotion campaign not found"));
        requireCampaignCurrentlyActive(campaign);
        return campaign;
    }

    private PromotionCampaign requireActiveCampaignForUpdate(String publicId) {
        PromotionCampaign campaign = campaignRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound(BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                        "Promotion campaign not found"));
        requireCampaignCurrentlyActive(campaign);
        return campaign;
    }

    private void requireCampaignCurrentlyActive(PromotionCampaign campaign) {
        Instant now = Instant.now();
        if (campaign.getStatus() != CampaignStatus.ACTIVE
                || Boolean.TRUE.equals(campaign.getKillSwitch())
                || now.isBefore(campaign.getStartAt())
                || !now.isBefore(campaign.getEndAt())) {
            throw badRequest(BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                    "Promotion campaign is not active");
        }
    }

    private void requireCampaignCapacity(PromotionCampaign campaign, BigDecimal discount) {
        if (campaign.getMaxRedemptions() != null
                && campaign.getRedemptionCount() >= campaign.getMaxRedemptions()) {
            throw badRequest(BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                    "Campaign redemption limit has been reached");
        }
        if (campaign.getBudgetAmount() != null && campaign.getBudgetAmount().signum() > 0
                && campaign.getBudgetRemaining().compareTo(discount) < 0) {
            throw badRequest(BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                    "Campaign budget is insufficient");
        }
    }

    private void requireCampaignUserCapacity(PromotionCampaign campaign, String userPublicId) {
        long couponUsage = couponRedemptionRepository
                .countByCampaignPublicIdAndUserPublicIdAndStatusIn(
                        campaign.getPublicId(), userPublicId, CONSUMED_STATUSES);
        long voucherUsage = voucherRedemptionRepository
                .countByCampaignPublicIdAndRedeemedByAndStatusIn(
                        campaign.getPublicId(), userPublicId, CONSUMED_STATUSES);
        if (couponUsage + voucherUsage >= campaign.getMaxRedemptionsPerUser()) {
            throw badRequest(
                    BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Campaign redemption limit for this user has been reached");
        }
    }

    private void consumeCampaignCapacity(PromotionCampaign campaign, BigDecimal discount) {
        requireCampaignRedemptionCapacity(campaign);
        if (campaign.getBudgetAmount() != null && campaign.getBudgetAmount().signum() > 0
                && campaign.getBudgetReserved().compareTo(discount) < 0) {
            throw badRequest(BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                    "Reserved campaign budget is inconsistent");
        }
        campaign.setBudgetReserved(campaign.getBudgetReserved().subtract(discount).max(BigDecimal.ZERO));
        campaign.setRedemptionCount(campaign.getRedemptionCount() + 1);
        campaign.setBudgetUsed(campaign.getBudgetUsed().add(discount));
        if (campaign.getBudgetAmount() != null && campaign.getBudgetAmount().signum() > 0) {
            campaign.setBudgetRemaining(campaign.getBudgetRemaining().subtract(discount));
        }
    }

    private void requireCampaignRedemptionCapacity(PromotionCampaign campaign) {
        if (campaign.getMaxRedemptions() != null
                && campaign.getRedemptionCount() >= campaign.getMaxRedemptions()) {
            throw badRequest(BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                    "Campaign redemption limit has been reached");
        }
    }

    private void requireReservationReference(BenefitRedeemRequest request) {
        if (request.getReservationPublicId() == null || request.getReservationPublicId().isBlank()) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "reservationPublicId is required when confirming a reservation");
        }
    }

    private void requireSameReservation(
            String existingReservationPublicId, String expectedReservationPublicId) {
        if (!java.util.Objects.equals(existingReservationPublicId, expectedReservationPublicId)) {
            throw new BusinessException(
                    BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Transaction was already redeemed by another reservation",
                    HttpStatus.CONFLICT);
        }
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            JsonNode parsed = objectMapper.readTree(value);
            // H2 and some legacy MySQL writes may return a JSON column as a
            // JSON-encoded string. Normalize it so evaluation is stable
            // across transactions and database engines.
            if (parsed != null && parsed.isTextual()) {
                parsed = objectMapper.readTree(parsed.asText());
            }
            return parsed;
        } catch (JsonProcessingException exception) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Benefit JSON configuration is invalid");
        }
    }

    private String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) return value.asText();
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                try {
                    return value.decimalValue();
                } catch (RuntimeException ignored) {
                    try {
                        return new BigDecimal(value.asText());
                    } catch (NumberFormatException ignoredAgain) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }

    private BusinessException notFound(String code, String message) {
        return new BusinessException(code, message, HttpStatus.NOT_FOUND);
    }
}

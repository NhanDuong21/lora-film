package com.project.promotionservice.benefit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.benefit.dto.request.RedemptionRequests.BenefitValidationRequest;
import com.project.promotionservice.benefit.dto.request.RedemptionRequests.ReservedRedemptionRequest;
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
import com.project.promotionservice.benefit.service.BenefitConditionEvaluator;
import com.project.promotionservice.benefit.service.RedemptionService;
import com.project.promotionservice.benefit.service.VerifiedPhoneNormalizer;
import com.project.promotionservice.benefit.specification.BenefitSpecifications;
import com.project.promotionservice.common.audit.Auditable;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.common.time.DatabaseTimeProvider;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
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
    private final BenefitConditionEvaluator conditionEvaluator;
    private final VerifiedPhoneNormalizer phoneNormalizer;
    private final DatabaseTimeProvider databaseTimeProvider;

    public RedemptionServiceImpl(CouponRepository couponRepository,
                                 VoucherRepository voucherRepository,
                                 CouponRedemptionRepository couponRedemptionRepository,
                                 VoucherRedemptionRepository voucherRedemptionRepository,
                                 BenefitCampaignRepository campaignRepository,
                                  BenefitMapper mapper,
                                  BenefitEventService eventService,
                                  ObjectMapper objectMapper,
                                  BenefitConditionEvaluator conditionEvaluator,
                                  VerifiedPhoneNormalizer phoneNormalizer,
                                  DatabaseTimeProvider databaseTimeProvider) {
        this.couponRepository = couponRepository;
        this.voucherRepository = voucherRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
        this.voucherRedemptionRepository = voucherRedemptionRepository;
        this.campaignRepository = campaignRepository;
        this.mapper = mapper;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.conditionEvaluator = conditionEvaluator;
        this.phoneNormalizer = phoneNormalizer;
        this.databaseTimeProvider = databaseTimeProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResponse validateCoupon(BenefitValidationRequest request) {
        request.setCustomerPhone(phoneNormalizer.normalizeNullable(request.getCustomerPhone()));
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(request.getCode())
                .orElseThrow(() -> notFound(BenefitErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
        return validateCouponEntity(coupon, request);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResponse validateVoucher(BenefitValidationRequest request) {
        request.setCustomerPhone(phoneNormalizer.normalizeNullable(request.getCustomerPhone()));
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
    public RedemptionResponse confirmReservedCoupon(ReservedRedemptionRequest request, String actor) {
        requireReservationReference(request);
        requireValidReservedAmounts(request);
        return confirmCouponRedemption(request, actor);
    }

    private RedemptionResponse confirmCouponRedemption(
            ReservedRedemptionRequest request, String actor) {
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

        PromotionCampaign campaign = requireCampaignForUpdate(coupon.getCampaignPublicId());
        consumeCampaignCapacity(campaign, request.getDiscountAmount());

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
        redemption.setDiscountAmount(money(request.getDiscountAmount()));
        redemption.setOriginalAmount(money(request.getOriginalAmount()));
        redemption.setFinalAmount(money(request.getFinalAmount()));
        redemption.setConfirmedAt(databaseTimeProvider.now());
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
    public RedemptionResponse confirmReservedVoucher(ReservedRedemptionRequest request, String actor) {
        requireReservationReference(request);
        requireValidReservedAmounts(request);
        return confirmVoucherRedemption(request, actor);
    }

    private RedemptionResponse confirmVoucherRedemption(
            ReservedRedemptionRequest request, String actor) {
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
            campaign = requireCampaignForUpdate(voucher.getCampaignPublicId());
        }
        if (campaign != null) {
            consumeCampaignCapacity(campaign, request.getDiscountAmount());
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
        redemption.setOriginalAmount(money(request.getOriginalAmount()));
        redemption.setDiscountAmount(money(request.getDiscountAmount()));
        redemption.setFinalAmount(money(request.getFinalAmount()));
        redemption.setConfirmedAt(databaseTimeProvider.now());
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
        Instant now = databaseTimeProvider.now();
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
        conditionEvaluator.evaluate(conditions, request);
        BigDecimal discount = calculateDiscount(
                readJson(coupon.getActionsJson()), null,
                request.getOriginalAmount(), request.getContextJson());
        requireCampaignCapacity(campaign, discount);
        return validResponse(
                RedemptionType.COUPON, coupon.getPublicId(), coupon.getCode(),
                request.getOriginalAmount(), discount);
    }

    private ValidationResponse validateVoucherEntity(Voucher voucher, BenefitValidationRequest request) {
        Instant now = databaseTimeProvider.now();
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
        conditionEvaluator.evaluate(readJson(voucher.getConditionsJson()), request);
        validateVoucherPolicy(voucher, request);
        BigDecimal discount = calculateDiscount(
                readJson(voucher.getActionsJson()), voucher,
                request.getOriginalAmount(), request.getContextJson());
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

    private BigDecimal calculateDiscount(
            JsonNode actions,
            Voucher voucher,
            BigDecimal originalAmount,
            JsonNode context) {
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
        if (normalizedType.equals("PERCENTAGE") || normalizedType.equals("PERCENT")) {
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
        } else if (normalizedType.equals(VoucherType.FREE_TICKET.name())) {
            discount = requiredEligibleAmount(context, "ticketAmount", originalAmount);
        } else if (normalizedType.equals(VoucherType.FREE_COMBO.name())) {
            discount = requiredEligibleAmount(context, "comboAmount", originalAmount);
        } else if (normalizedType.equals("FREE")
                || normalizedType.equals("FULL_DISCOUNT")) {
            discount = originalAmount;
        } else if (normalizedType.equals("FIXED_AMOUNT")
                || normalizedType.equals("AMOUNT")
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

    private BigDecimal requiredEligibleAmount(
            JsonNode context, String field, BigDecimal originalAmount) {
        JsonNode value = context == null ? null : context.get(field);
        BigDecimal eligibleAmount;
        try {
            eligibleAmount = value == null || value.isNull()
                    ? null
                    : (value.isNumber()
                    ? value.decimalValue()
                    : new BigDecimal(value.asText()));
        } catch (NumberFormatException exception) {
            throw badRequest(
                    BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    field + " must be a valid monetary amount");
        }
        if (eligibleAmount == null
                || eligibleAmount.signum() <= 0
                || eligibleAmount.compareTo(originalAmount) > 0) {
            throw badRequest(
                    BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    field + " must be greater than zero and not exceed originalAmount");
        }
        return money(eligibleAmount);
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

    private PromotionCampaign requireCampaignForUpdate(String publicId) {
        return campaignRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound(BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                        "Promotion campaign not found"));
    }

    private void requireCampaignCurrentlyActive(PromotionCampaign campaign) {
        Instant now = databaseTimeProvider.now();
        if (campaign.getStatus() != CampaignStatus.ACTIVE
                || campaign.getLegalStatus() != LegalStatus.PASSED
                || Boolean.TRUE.equals(campaign.getKillSwitch())
                || campaign.getBudgetAmount() == null
                || campaign.getBudgetAmount().signum() <= 0
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
        if (Boolean.TRUE.equals(campaign.getAutoPauseWhenBudgetExceeded())
                && campaign.getBudgetAmount() != null
                && campaign.getBudgetAmount().signum() > 0
                && campaign.getBudgetUsed().compareTo(
                        campaign.getBudgetAmount().multiply(new BigDecimal("0.98"))) >= 0) {
            campaign.setStatus(CampaignStatus.PAUSED);
        }
    }

    private void requireCampaignRedemptionCapacity(PromotionCampaign campaign) {
        if (campaign.getMaxRedemptions() != null
                && campaign.getRedemptionCount() >= campaign.getMaxRedemptions()) {
            throw badRequest(BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                    "Campaign redemption limit has been reached");
        }
    }

    private void requireReservationReference(ReservedRedemptionRequest request) {
        if (request.getReservationPublicId() == null || request.getReservationPublicId().isBlank()) {
            throw badRequest(BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "reservationPublicId is required when confirming a reservation");
        }
    }

    private void requireValidReservedAmounts(ReservedRedemptionRequest request) {
        BigDecimal original = money(request.getOriginalAmount());
        BigDecimal discount = money(request.getDiscountAmount());
        BigDecimal finalAmount = money(request.getFinalAmount());
        if (original.signum() < 0
                || discount.signum() < 0
                || discount.compareTo(original) > 0
                || finalAmount.compareTo(original.subtract(discount)) != 0) {
            throw badRequest(
                    BenefitErrorCode.BENEFIT_CONDITION_NOT_MET,
                    "Reserved redemption amount snapshot is inconsistent");
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

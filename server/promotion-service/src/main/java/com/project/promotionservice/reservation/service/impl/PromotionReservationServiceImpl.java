package com.project.promotionservice.reservation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.promotionservice.benefit.dto.request.RedemptionRequests.BenefitRedeemRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.RedemptionResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.ValidationResponse;
import com.project.promotionservice.benefit.entity.Coupon;
import com.project.promotionservice.benefit.entity.Voucher;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import com.project.promotionservice.benefit.exception.BenefitErrorCode;
import com.project.promotionservice.benefit.mapper.BenefitMapper;
import com.project.promotionservice.benefit.repository.BenefitCampaignRepository;
import com.project.promotionservice.benefit.repository.CouponRedemptionRepository;
import com.project.promotionservice.benefit.repository.CouponRepository;
import com.project.promotionservice.benefit.repository.VoucherRedemptionRepository;
import com.project.promotionservice.benefit.repository.VoucherRepository;
import com.project.promotionservice.benefit.service.BenefitEventService;
import com.project.promotionservice.benefit.service.RedemptionService;
import com.project.promotionservice.common.audit.Auditable;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RollbackRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.entity.PromotionReservation;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.exception.ReservationErrorCode;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Service
public class PromotionReservationServiceImpl implements PromotionReservationService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;
    private static final int EXPIRATION_BATCH_SIZE = 100;
    private static final EnumSet<RedemptionStatus> CONSUMED_STATUSES =
            EnumSet.of(RedemptionStatus.SUCCESS, RedemptionStatus.CONFIRMED);

    private final PromotionReservationRepository reservationRepository;
    private final CouponRepository couponRepository;
    private final VoucherRepository voucherRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final BenefitCampaignRepository campaignRepository;
    private final RedemptionService redemptionService;
    private final BenefitEventService eventService;
    private final BenefitMapper benefitMapper;
    private final ObjectMapper objectMapper;

    public PromotionReservationServiceImpl(
            PromotionReservationRepository reservationRepository,
            CouponRepository couponRepository,
            VoucherRepository voucherRepository,
            CouponRedemptionRepository couponRedemptionRepository,
            VoucherRedemptionRepository voucherRedemptionRepository,
            BenefitCampaignRepository campaignRepository,
            RedemptionService redemptionService,
            BenefitEventService eventService,
            BenefitMapper benefitMapper,
            ObjectMapper objectMapper) {
        this.reservationRepository = reservationRepository;
        this.couponRepository = couponRepository;
        this.voucherRepository = voucherRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
        this.voucherRedemptionRepository = voucherRedemptionRepository;
        this.campaignRepository = campaignRepository;
        this.redemptionService = redemptionService;
        this.eventService = eventService;
        this.benefitMapper = benefitMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    @Auditable(action = "RESERVE", entityType = "PROMOTION_RESERVATION")
    @Caching(evict = {
            @CacheEvict(cacheNames = "promotions", allEntries = true),
            @CacheEvict(cacheNames = "vouchers", allEntries = true)
    })
    public ReservationResponse reserve(ReserveRequest request, String idempotencyKey, String actor) {
        String reservationCode = reservationCode(idempotencyKey);
        Instant now = Instant.now();

        PromotionReservation existing = reservationRepository
                .findByReservationCodeAndDeletedAtIsNull(reservationCode)
                .orElse(null);
        if (existing != null
                && !(existing.getStatus() == ReservationStatus.ACTIVE
                && !now.isBefore(existing.getReservationExpiredAt()))) {
            existing = requireForUpdate(existing.getPublicId());
            assertSameRequest(existing, request);
            return response(existing, existingRedemption(existing));
        }

        LockedBenefit benefit = lockBenefit(request);

        existing = reservationRepository.findByReservationCodeAndDeletedAtIsNull(reservationCode)
                .orElse(null);
        if (existing != null) {
            assertSameRequest(existing, request);
            if (existing.getStatus() == ReservationStatus.ACTIVE
                    && !now.isBefore(existing.getReservationExpiredAt())) {
                expire(existing, now, actor);
            }
            return response(existing, existingRedemption(existing));
        }

        requireNoConcurrentHold(benefit, now);
        PromotionCampaign lockedCampaign = benefit.campaignPublicId() == null
                ? null : lockActiveCampaign(benefit.campaignPublicId(), now);

        ValidationResponse validation = benefit.type() == RedemptionType.COUPON
                ? redemptionService.validateCoupon(request)
                : redemptionService.validateVoucher(request);

        if (lockedCampaign != null) {
            reserveCampaignCapacity(
                    lockedCampaign, request.getUserPublicId(),
                    validation.getDiscountAmount(), now, actor);
        }

        PromotionReservation reservation = new PromotionReservation();
        reservation.setReservationCode(reservationCode);
        reservation.setBookingPublicId(request.getBookingPublicId());
        reservation.setOrderPublicId(request.getOrderPublicId());
        reservation.setPaymentPublicId(request.getPaymentPublicId());
        reservation.setUserPublicId(request.getUserPublicId());
        reservation.setCampaignPublicId(benefit.campaignPublicId());
        reservation.setCouponPublicId(benefit.type() == RedemptionType.COUPON
                ? benefit.publicId() : null);
        reservation.setVoucherPublicId(benefit.type() == RedemptionType.VOUCHER
                ? benefit.publicId() : null);
        reservation.setReservationType(benefit.type());
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setOriginalAmount(money(validation.getOriginalAmount()));
        reservation.setDiscountAmount(money(validation.getDiscountAmount()));
        reservation.setFinalAmount(money(validation.getFinalAmount()));
        reservation.setCurrency(validation.getCurrency());
        reservation.setReservationStartedAt(now);
        reservation.setReservationExpiredAt(now.plusSeconds(request.getHoldDurationSeconds()));
        reservation.setMetadataJson(metadata(request));
        reservation.setCreatedBy(actor);
        reservation.setUpdatedBy(actor);

        PromotionReservation saved;
        try {
            saved = reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException exception) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                    "A concurrent request is already using this idempotency key");
        }
        ReservationResponse response = response(saved, null);
        eventService.record(
                "PROMOTION_RESERVATION", saved.getPublicId(),
                "PROMOTION_RESERVED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "CONFIRM", entityType = "PROMOTION_RESERVATION")
    @Caching(evict = {
            @CacheEvict(cacheNames = "promotions", allEntries = true),
            @CacheEvict(cacheNames = "vouchers", allEntries = true)
    })
    public ReservationResponse confirm(ConfirmRequest request, String idempotencyKey, String actor) {
        requireIdempotencyKey(idempotencyKey);
        PromotionReservation reservation = requireForUpdate(request.getReservationPublicId());

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            if (!Objects.equals(reservation.getPaymentPublicId(), request.getPaymentPublicId())) {
                throw conflict(ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                        "Reservation was confirmed with another payment");
            }
            return response(reservation, requireExistingRedemption(reservation));
        }
        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            return response(reservation, null);
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw conflict(ReservationErrorCode.RESERVATION_INVALID_STATE,
                    "Cancelled reservation cannot be confirmed");
        }

        Instant now = Instant.now();
        if (!now.isBefore(reservation.getReservationExpiredAt())) {
            expire(reservation, now, actor);
            ReservationResponse expiredResponse = response(reservation, null);
            eventService.record(
                    "PROMOTION_RESERVATION", reservation.getPublicId(),
                    "PROMOTION_EXPIRED", expiredResponse, actor);
            return expiredResponse;
        }

        BenefitRedeemRequest redeemRequest = redeemRequest(reservation, request.getPaymentPublicId());
        RedemptionResponse redemption = reservation.getReservationType() == RedemptionType.COUPON
                ? redemptionService.confirmReservedCoupon(redeemRequest, actor)
                : redemptionService.confirmReservedVoucher(redeemRequest, actor);
        if (!Objects.equals(redemption.getReservationPublicId(), reservation.getPublicId())) {
            throw conflict(ReservationErrorCode.RESERVATION_CONFLICT,
                    "Redemption belongs to another reservation");
        }

        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.setPaymentPublicId(request.getPaymentPublicId());
        reservation.setConfirmedAt(now);
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);

        ReservationResponse response = response(reservation, redemption);
        eventService.record(
                "PROMOTION_RESERVATION", reservation.getPublicId(),
                "PROMOTION_CONFIRMED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "ROLLBACK", entityType = "PROMOTION_RESERVATION")
    @Caching(evict = {
            @CacheEvict(cacheNames = "promotions", allEntries = true),
            @CacheEvict(cacheNames = "vouchers", allEntries = true)
    })
    public ReservationResponse rollback(RollbackRequest request, String idempotencyKey, String actor) {
        requireIdempotencyKey(idempotencyKey);
        PromotionReservation reservation = requireForUpdate(request.getReservationPublicId());

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.EXPIRED) {
            return response(reservation, null);
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw conflict(ReservationErrorCode.RESERVATION_COMPLETED,
                    "Completed reservation cannot be rolled back; use refund or compensation");
        }

        releaseCampaignCapacity(reservation, actor);
        Instant now = Instant.now();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(now);
        reservation.setCancelledReason(request.getReason());
        reservation.setRollbackAt(now);
        reservation.setRollbackReason(request.getReason());
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);

        ReservationResponse response = response(reservation, null);
        eventService.record(
                "PROMOTION_RESERVATION", reservation.getPublicId(),
                "PROMOTION_ROLLED_BACK", response, actor);
        return response;
    }

    @Override
    @Transactional
    public int expireDueReservations(String actor) {
        Instant now = Instant.now();
        List<PromotionReservation> due = reservationRepository
                .findByStatusAndReservationExpiredAtLessThanEqualAndDeletedAtIsNull(
                        ReservationStatus.ACTIVE, now, PageRequest.of(0, EXPIRATION_BATCH_SIZE));
        int expiredCount = 0;
        for (PromotionReservation candidate : due) {
            PromotionReservation reservation = reservationRepository
                    .findByPublicIdForUpdate(candidate.getPublicId())
                    .orElse(null);
            if (reservation == null
                    || reservation.getStatus() != ReservationStatus.ACTIVE
                    || now.isBefore(reservation.getReservationExpiredAt())) {
                continue;
            }
            expire(reservation, now, actor);
            ReservationResponse response = response(reservation, null);
            eventService.record(
                    "PROMOTION_RESERVATION", reservation.getPublicId(),
                    "PROMOTION_EXPIRED", response, actor);
            expiredCount++;
        }
        return expiredCount;
    }

    private LockedBenefit lockBenefit(ReserveRequest request) {
        if (request.getBenefitType() == RedemptionType.COUPON) {
            Coupon coupon = couponRepository.findByCodeForUpdate(request.getCode())
                    .orElseThrow(() -> notFound(BenefitErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
            return new LockedBenefit(
                    RedemptionType.COUPON, coupon.getPublicId(),
                    coupon.getCampaignPublicId(), coupon.getCode());
        }
        Voucher voucher = voucherRepository.findByCodeForUpdate(request.getCode())
                .orElseThrow(() -> notFound(BenefitErrorCode.VOUCHER_NOT_FOUND, "Voucher not found"));
        return new LockedBenefit(
                RedemptionType.VOUCHER, voucher.getPublicId(),
                voucher.getCampaignPublicId(), voucher.getCode());
    }

    private void requireNoConcurrentHold(LockedBenefit benefit, Instant now) {
        long activeReservations = benefit.type() == RedemptionType.COUPON
                ? reservationRepository
                .countByCouponPublicIdAndStatusAndReservationExpiredAtAfterAndDeletedAtIsNull(
                        benefit.publicId(), ReservationStatus.ACTIVE, now)
                : reservationRepository
                .countByVoucherPublicIdAndStatusAndReservationExpiredAtAfterAndDeletedAtIsNull(
                        benefit.publicId(), ReservationStatus.ACTIVE, now);
        if (activeReservations > 0) {
            throw conflict(ReservationErrorCode.RESERVATION_CONFLICT,
                    "Benefit is already held by another active reservation");
        }
    }

    private PromotionCampaign lockActiveCampaign(String campaignPublicId, Instant now) {
        PromotionCampaign campaign = campaignRepository.findByPublicIdForUpdate(campaignPublicId)
                .orElseThrow(() -> notFound(
                        BenefitErrorCode.CAMPAIGN_NOT_ACTIVE, "Promotion campaign not found"));
        requireActiveCampaign(campaign, now);
        return campaign;
    }

    private void reserveCampaignCapacity(
            PromotionCampaign campaign, String userPublicId,
            BigDecimal discountAmount, Instant now, String actor) {
        String campaignPublicId = campaign.getPublicId();

        long activeReservations = reservationRepository
                .countByCampaignPublicIdAndStatusAndReservationExpiredAtAfterAndDeletedAtIsNull(
                        campaignPublicId, ReservationStatus.ACTIVE, now);
        if (campaign.getMaxRedemptions() != null
                && campaign.getRedemptionCount() + activeReservations >= campaign.getMaxRedemptions()) {
            throw conflict(ReservationErrorCode.RESERVATION_CONFLICT,
                    "Campaign redemption capacity is fully reserved");
        }
        long activeUserReservations = reservationRepository
                .countByCampaignPublicIdAndUserPublicIdAndStatusAndReservationExpiredAtAfterAndDeletedAtIsNull(
                        campaignPublicId, userPublicId, ReservationStatus.ACTIVE, now);
        long couponUsage = couponRedemptionRepository
                .countByCampaignPublicIdAndUserPublicIdAndStatusIn(
                        campaignPublicId, userPublicId, CONSUMED_STATUSES);
        long voucherUsage = voucherRedemptionRepository
                .countByCampaignPublicIdAndRedeemedByAndStatusIn(
                        campaignPublicId, userPublicId, CONSUMED_STATUSES);
        if (couponUsage + voucherUsage + activeUserReservations
                >= campaign.getMaxRedemptionsPerUser()) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Campaign capacity for this user is fully reserved");
        }
        if (campaign.getBudgetAmount() != null && campaign.getBudgetAmount().signum() > 0) {
            BigDecimal available = campaign.getBudgetRemaining().subtract(campaign.getBudgetReserved());
            if (available.compareTo(discountAmount) < 0) {
                throw conflict(ReservationErrorCode.RESERVATION_CONFLICT,
                        "Campaign budget is fully reserved");
            }
            campaign.setBudgetReserved(campaign.getBudgetReserved().add(discountAmount));
            campaign.setUpdatedBy(actor);
            campaignRepository.save(campaign);
        }
    }

    private void requireActiveCampaign(PromotionCampaign campaign, Instant now) {
        if (campaign.getStatus() != CampaignStatus.ACTIVE
                || Boolean.TRUE.equals(campaign.getKillSwitch())
                || now.isBefore(campaign.getStartAt())
                || !now.isBefore(campaign.getEndAt())) {
            throw new BusinessException(
                    BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                    "Promotion campaign is not active",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void expire(PromotionReservation reservation, Instant now, String actor) {
        releaseCampaignCapacity(reservation, actor);
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservation.setCancelledAt(now);
        reservation.setCancelledReason("Reservation expired");
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);
    }

    private void releaseCampaignCapacity(PromotionReservation reservation, String actor) {
        if (reservation.getCampaignPublicId() == null) {
            return;
        }
        PromotionCampaign campaign = campaignRepository
                .findByPublicIdForUpdate(reservation.getCampaignPublicId())
                .orElseThrow(() -> notFound(
                        BenefitErrorCode.CAMPAIGN_NOT_ACTIVE, "Promotion campaign not found"));
        if (campaign.getBudgetAmount() != null && campaign.getBudgetAmount().signum() > 0) {
            campaign.setBudgetReserved(
                    campaign.getBudgetReserved().subtract(reservation.getDiscountAmount()).max(BigDecimal.ZERO));
            campaign.setUpdatedBy(actor);
            campaignRepository.save(campaign);
        }
    }

    private PromotionReservation requireForUpdate(String publicId) {
        return reservationRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound(
                        ReservationErrorCode.RESERVATION_NOT_FOUND, "Reservation not found"));
    }

    private RedemptionResponse existingRedemption(PromotionReservation reservation) {
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            return null;
        }
        return requireExistingRedemption(reservation);
    }

    private RedemptionResponse requireExistingRedemption(PromotionReservation reservation) {
        if (reservation.getReservationType() == RedemptionType.COUPON) {
            return couponRedemptionRepository
                    .findByReservationPublicIdAndDeletedAtIsNull(reservation.getPublicId())
                    .map(benefitMapper::toRedemptionResponse)
                    .orElseThrow(() -> conflict(
                            ReservationErrorCode.RESERVATION_CONFLICT,
                            "Completed reservation has no coupon redemption"));
        }
        return voucherRedemptionRepository
                .findByReservationPublicIdAndDeletedAtIsNull(reservation.getPublicId())
                .map(redemption -> {
                    RedemptionResponse response = benefitMapper.toRedemptionResponse(redemption);
                    response.setCode(benefitCode(reservation));
                    return response;
                })
                .orElseThrow(() -> conflict(
                        ReservationErrorCode.RESERVATION_CONFLICT,
                        "Completed reservation has no voucher redemption"));
    }

    private BenefitRedeemRequest redeemRequest(
            PromotionReservation reservation, String paymentPublicId) {
        BenefitRedeemRequest request = new BenefitRedeemRequest();
        request.setCode(benefitCode(reservation));
        request.setUserPublicId(reservation.getUserPublicId());
        request.setOriginalAmount(reservation.getOriginalAmount());
        request.setReservationPublicId(reservation.getPublicId());
        request.setBookingPublicId(reservation.getBookingPublicId());
        request.setOrderPublicId(reservation.getOrderPublicId());
        request.setPaymentPublicId(paymentPublicId);
        JsonNode metadata = readJson(reservation.getMetadataJson());
        JsonNode context = metadata == null ? null : metadata.get("context");
        request.setContextJson(context);
        if (metadata != null && metadata.hasNonNull("customerPhone")) {
            request.setCustomerPhone(metadata.get("customerPhone").asText());
        }
        return request;
    }

    private String benefitCode(PromotionReservation reservation) {
        if (reservation.getReservationType() == RedemptionType.COUPON) {
            return couponRepository.findByPublicIdAndDeletedAtIsNull(reservation.getCouponPublicId())
                    .map(Coupon::getCode)
                    .orElseThrow(() -> notFound(BenefitErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
        }
        return voucherRepository.findByPublicIdAndDeletedAtIsNull(reservation.getVoucherPublicId())
                .map(Voucher::getCode)
                .orElseThrow(() -> notFound(BenefitErrorCode.VOUCHER_NOT_FOUND, "Voucher not found"));
    }

    private void assertSameRequest(PromotionReservation reservation, ReserveRequest request) {
        boolean sameBenefit = reservation.getReservationType() == request.getBenefitType()
                && benefitCode(reservation).equalsIgnoreCase(request.getCode());
        boolean samePayload = Objects.equals(reservation.getUserPublicId(), request.getUserPublicId())
                && money(reservation.getOriginalAmount()).compareTo(money(request.getOriginalAmount())) == 0
                && Objects.equals(reservation.getBookingPublicId(), request.getBookingPublicId())
                && Objects.equals(reservation.getOrderPublicId(), request.getOrderPublicId())
                && Objects.equals(reservation.getPaymentPublicId(), request.getPaymentPublicId())
                && reservation.getReservationExpiredAt().getEpochSecond()
                - reservation.getReservationStartedAt().getEpochSecond()
                == request.getHoldDurationSeconds()
                && sameMetadata(reservation, request);
        if (!sameBenefit || !samePayload) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                    "Idempotency key was already used for another reservation request");
        }
    }

    private boolean sameMetadata(PromotionReservation reservation, ReserveRequest request) {
        JsonNode metadata = readJson(reservation.getMetadataJson());
        String existingPhone = metadata != null && metadata.hasNonNull("customerPhone")
                ? metadata.get("customerPhone").asText() : null;
        JsonNode existingContext = metadata == null ? null : metadata.get("context");
        return Objects.equals(existingPhone, blankToNull(request.getCustomerPhone()))
                && Objects.equals(existingContext, request.getContextJson());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String metadata(ReserveRequest request) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("schemaVersion", 1);
        if (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank()) {
            metadata.put("customerPhone", request.getCustomerPhone());
        }
        if (request.getContextJson() != null && !request.getContextJson().isNull()) {
            metadata.set("context", request.getContextJson());
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Unable to serialize reservation metadata",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            JsonNode parsed = objectMapper.readTree(value);
            if (parsed != null && parsed.isTextual()) {
                parsed = objectMapper.readTree(parsed.asText());
            }
            return parsed;
        } catch (JsonProcessingException exception) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Reservation metadata is invalid");
        }
    }

    private ReservationResponse response(
            PromotionReservation reservation, RedemptionResponse redemption) {
        ReservationResponse response = new ReservationResponse();
        response.setPublicId(reservation.getPublicId());
        response.setReservationCode(reservation.getReservationCode());
        response.setReservationType(reservation.getReservationType());
        response.setStatus(reservation.getStatus());
        response.setCampaignPublicId(reservation.getCampaignPublicId());
        response.setBenefitPublicId(reservation.getReservationType() == RedemptionType.COUPON
                ? reservation.getCouponPublicId() : reservation.getVoucherPublicId());
        response.setBookingPublicId(reservation.getBookingPublicId());
        response.setOrderPublicId(reservation.getOrderPublicId());
        response.setPaymentPublicId(reservation.getPaymentPublicId());
        response.setUserPublicId(reservation.getUserPublicId());
        response.setOriginalAmount(reservation.getOriginalAmount());
        response.setDiscountAmount(reservation.getDiscountAmount());
        response.setFinalAmount(reservation.getFinalAmount());
        response.setCurrency(reservation.getCurrency());
        response.setReservationStartedAt(reservation.getReservationStartedAt());
        response.setReservationExpiredAt(reservation.getReservationExpiredAt());
        response.setConfirmedAt(reservation.getConfirmedAt());
        response.setCancelledAt(reservation.getCancelledAt());
        response.setCancelledReason(reservation.getCancelledReason());
        response.setRollbackAt(reservation.getRollbackAt());
        response.setRollbackReason(reservation.getRollbackReason());
        JsonNode metadata = readJson(reservation.getMetadataJson());
        response.setMetadataJson(metadata == null ? null : metadata.get("context"));
        response.setRedemption(redemption);
        return response;
    }

    private String reservationCode(String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.trim().getBytes(StandardCharsets.UTF_8));
            return "RSV-" + HexFormat.of().formatHex(digest, 0, 24).toUpperCase();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                    "X-Idempotency-Key is required and must not exceed 255 characters",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }

    private BusinessException notFound(String code, String message) {
        return new BusinessException(code, message, HttpStatus.NOT_FOUND);
    }

    private record LockedBenefit(
            RedemptionType type, String publicId, String campaignPublicId, String code) {
    }
}

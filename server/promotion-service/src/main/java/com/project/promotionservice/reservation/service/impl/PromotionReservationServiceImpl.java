package com.project.promotionservice.reservation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.promotionservice.benefit.dto.request.RedemptionRequests.ReservedRedemptionRequest;
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
import com.project.promotionservice.benefit.service.VerifiedPhoneNormalizer;
import com.project.promotionservice.common.audit.Auditable;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.common.time.DatabaseTimeProvider;
import com.project.promotionservice.common.monitoring.PromotionMetricsManager;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RefreshRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RuntimeValidationRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.entity.PromotionReservation;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.exception.ReservationErrorCode;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import com.project.promotionservice.reservation.service.ReservationLockManager;
import com.project.promotionservice.reservation.specification.ReservationSpecifications;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Service
public class PromotionReservationServiceImpl implements PromotionReservationService {

    private static final Logger log =
            LoggerFactory.getLogger(PromotionReservationServiceImpl.class);
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;
    private static final int EXPIRATION_BATCH_SIZE = 100;
    private static final int MAX_RESERVATION_LIFETIME_SECONDS = 1800;
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
    private final ReservationLockManager lockManager;
    private final CacheManager cacheManager;
    private final VerifiedPhoneNormalizer phoneNormalizer;
    private final TransactionTemplate requiresNewTransaction;
    private final DatabaseTimeProvider databaseTimeProvider;
    private final PromotionMetricsManager metricsManager;

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
            ObjectMapper objectMapper,
            ReservationLockManager lockManager,
            CacheManager cacheManager,
            VerifiedPhoneNormalizer phoneNormalizer,
            PlatformTransactionManager transactionManager,
            DatabaseTimeProvider databaseTimeProvider,
            PromotionMetricsManager metricsManager) {
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
        this.lockManager = lockManager;
        this.cacheManager = cacheManager;
        this.phoneNormalizer = phoneNormalizer;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.databaseTimeProvider = databaseTimeProvider;
        this.metricsManager = metricsManager;
    }

    @Override
    @Transactional
    @Auditable(action = "RESERVE", entityType = "PROMOTION_RESERVATION")
    @Caching(evict = {
            @CacheEvict(cacheNames = "promotions", allEntries = true),
            @CacheEvict(cacheNames = "vouchers", allEntries = true),
            @CacheEvict(cacheNames = "reservations", allEntries = true)
    })
    public ReservationResponse reserve(ReserveRequest request, String idempotencyKey, String actor) {
        request.setCustomerPhone(phoneNormalizer.normalizeNullable(request.getCustomerPhone()));
        String reservationCode = reservationCode(actor, idempotencyKey);
        Instant now = databaseInstant();

        PromotionReservation existing = reservationRepository
                .findByReservationCodeAndDeletedAtIsNull(reservationCode)
                .orElse(null);
        if (existing != null) {
            existing = requireForUpdate(existing.getPublicId());
            assertSameRequest(existing, request);
            if (existing.getStatus() == ReservationStatus.ACTIVE
                    && !now.isBefore(existing.getReservationExpiredAt())) {
                expireAndRecord(existing, now, actor);
            }
            return response(existing, existingRedemption(existing));
        }

        lockManager.lockCheckout(request.getOrderPublicId(), request.getBookingPublicId());
        LockedBenefit resolvedBenefit = findBenefit(request);
        lockManager.lockBenefit(resolvedBenefit.type(), resolvedBenefit.publicId());
        LockedBenefit benefit = lockBenefit(request);

        existing = reservationRepository.findByReservationCodeAndDeletedAtIsNull(reservationCode)
                .orElse(null);
        if (existing != null) {
            assertSameRequest(existing, request);
            if (existing.getStatus() == ReservationStatus.ACTIVE
                    && !now.isBefore(existing.getReservationExpiredAt())) {
                expireAndRecord(existing, now, actor);
            }
            return response(existing, existingRedemption(existing));
        }

        PromotionCampaign lockedCampaign = benefit.campaignPublicId() == null
                ? null : lockActiveCampaign(benefit.campaignPublicId(), now);

        ValidationResponse validation = benefit.type() == RedemptionType.COUPON
                ? redemptionService.validateCoupon(request)
                : redemptionService.validateVoucher(request);
        requireBenefitCapacityAvailable(benefit, request, now);

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
        reservation.setCustomerPhone(blankToNull(request.getCustomerPhone()));
        reservation.setReservationScopeKey(reservationScopeKey(request));
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
                    "Idempotency key or checkout already has an effective promotion reservation");
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
            @CacheEvict(cacheNames = "vouchers", allEntries = true),
            @CacheEvict(cacheNames = "reservations", allEntries = true)
    })
    public ReservationResponse confirm(
            String reservationPublicId,
            ConfirmRequest request,
            String idempotencyKey,
            String actor) {
        requireIdempotencyKey(idempotencyKey);
        lockManager.lockReservation(reservationPublicId);
        PromotionReservation reservation = requireForUpdate(reservationPublicId);

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
        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.RELEASED) {
            throw conflict(ReservationErrorCode.RESERVATION_INVALID_STATE,
                    "Released or cancelled reservation cannot be confirmed");
        }

        Instant now = databaseInstant();
        if (!now.isBefore(reservation.getReservationExpiredAt())) {
            return expireAndRecord(reservation, now, actor);
        }

        ReservedRedemptionRequest redeemRequest = redeemRequest(
                reservation, request.getPaymentPublicId());
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
                "RESERVATION_CONFIRMED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "RELEASE", entityType = "PROMOTION_RESERVATION")
    @Caching(evict = {
            @CacheEvict(cacheNames = "promotions", allEntries = true),
            @CacheEvict(cacheNames = "vouchers", allEntries = true),
            @CacheEvict(cacheNames = "reservations", allEntries = true)
    })
    public ReservationResponse release(
            String reservationPublicId,
            TransitionRequest request,
            String idempotencyKey,
            String actor) {
        requireIdempotencyKey(idempotencyKey);
        lockManager.lockReservation(reservationPublicId);
        PromotionReservation reservation = requireForUpdate(reservationPublicId);

        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            requireSameReason(reservation.getRollbackReason(), request.getReason());
            return response(reservation, null);
        }
        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            return response(reservation, null);
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw conflict(ReservationErrorCode.RESERVATION_COMPLETED,
                    "Completed reservation cannot be released; use refund or compensation");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw conflict(ReservationErrorCode.RESERVATION_INVALID_STATE,
                    "Cancelled reservation cannot be released");
        }

        releaseCampaignCapacity(reservation, actor);
        Instant now = databaseInstant();
        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReservationScopeKey(null);
        reservation.setRollbackAt(now);
        reservation.setRollbackReason(request.getReason());
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);

        ReservationResponse response = response(reservation, null);
        eventService.record(
                "PROMOTION_RESERVATION", reservation.getPublicId(),
                "RESERVATION_RELEASED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "CANCEL", entityType = "PROMOTION_RESERVATION")
    @Caching(evict = {
            @CacheEvict(cacheNames = "promotions", allEntries = true),
            @CacheEvict(cacheNames = "vouchers", allEntries = true),
            @CacheEvict(cacheNames = "reservations", allEntries = true)
    })
    public ReservationResponse cancel(
            String reservationPublicId,
            TransitionRequest request,
            String idempotencyKey,
            String actor) {
        requireIdempotencyKey(idempotencyKey);
        lockManager.lockReservation(reservationPublicId);
        PromotionReservation reservation = requireForUpdate(reservationPublicId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            requireSameReason(reservation.getCancelledReason(), request.getReason());
            return response(reservation, null);
        }
        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            return response(reservation, null);
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw conflict(ReservationErrorCode.RESERVATION_COMPLETED,
                    "Completed reservation cannot be cancelled; use refund or compensation");
        }
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            throw conflict(ReservationErrorCode.RESERVATION_INVALID_STATE,
                    "Released reservation cannot be cancelled");
        }

        releaseCampaignCapacity(reservation, actor);
        Instant now = databaseInstant();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setReservationScopeKey(null);
        reservation.setCancelledAt(now);
        reservation.setCancelledReason(request.getReason());
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);

        ReservationResponse response = response(reservation, null);
        eventService.record(
                "PROMOTION_RESERVATION", reservation.getPublicId(),
                "RESERVATION_CANCELLED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "REFRESH", entityType = "PROMOTION_RESERVATION")
    @CacheEvict(cacheNames = "reservations", allEntries = true)
    public ReservationResponse refresh(
            String reservationPublicId,
            RefreshRequest request,
            String idempotencyKey,
            String actor) {
        requireIdempotencyKey(idempotencyKey);
        lockManager.lockReservation(reservationPublicId);
        PromotionReservation reservation = requireForUpdate(reservationPublicId);

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            if (reservation.getStatus() == ReservationStatus.EXPIRED) {
                return response(reservation, null);
            }
            throw conflict(ReservationErrorCode.RESERVATION_INVALID_STATE,
                    "Only an active reservation can be refreshed");
        }

        Instant now = databaseInstant();
        if (!now.isBefore(reservation.getReservationExpiredAt())) {
            return expireAndRecord(reservation, now, actor);
        }

        Instant requestedExpiredAt = request.getRequestedExpiredAt()
                .truncatedTo(ChronoUnit.MICROS);
        if (requestedExpiredAt.equals(reservation.getReservationExpiredAt())) {
            return response(reservation, null);
        }
        if (!requestedExpiredAt.isAfter(reservation.getReservationExpiredAt())) {
            throw conflict(ReservationErrorCode.RESERVATION_INVALID_STATE,
                    "requestedExpiredAt must extend the current expiration time");
        }
        Instant maximumExpiredAt = maximumRefreshDeadline(reservation);
        if (requestedExpiredAt.isAfter(maximumExpiredAt)) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_INVALID_STATE,
                    "Reservation cannot be extended beyond its maximum lifetime or promotion validity",
                    HttpStatus.BAD_REQUEST);
        }

        validateReservationSnapshot(reservation);
        reservation.setReservationExpiredAt(requestedExpiredAt);
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);

        ReservationResponse response = response(reservation, null);
        eventService.record(
                "PROMOTION_RESERVATION", reservation.getPublicId(),
                "RESERVATION_REFRESHED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Cacheable(
            cacheNames = "reservations",
            key = "#reservationPublicId",
            unless = "#result.status.name() == 'ACTIVE'")
    public ReservationResponse getDetail(String reservationPublicId, String actor) {
        PromotionReservation reservation = reservationRepository
                .findByPublicIdAndDeletedAtIsNull(reservationPublicId)
                .orElseThrow(() -> notFound(
                        ReservationErrorCode.RESERVATION_NOT_FOUND, "Reservation not found"));
        Instant now = databaseInstant();
        if (reservation.getStatus() == ReservationStatus.ACTIVE
                && !now.isBefore(reservation.getReservationExpiredAt())) {
            lockManager.lockReservation(reservationPublicId);
            reservation = requireForUpdate(reservationPublicId);
            if (reservation.getStatus() == ReservationStatus.ACTIVE
                    && !now.isBefore(reservation.getReservationExpiredAt())) {
                return expireAndRecord(reservation, now, actor);
            }
        }
        return response(reservation, existingRedemption(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResponse validateRuntime(RuntimeValidationRequest request) {
        request.setCustomerPhone(phoneNormalizer.normalizeNullable(request.getCustomerPhone()));
        Instant now = databaseInstant();
        LockedBenefit benefit = findBenefit(request.getBenefitType(), request.getCode());
        ValidationResponse validation = benefit.type() == RedemptionType.COUPON
                ? redemptionService.validateCoupon(request)
                : redemptionService.validateVoucher(request);
        requireBenefitCapacityAvailable(benefit, request, now);
        if (benefit.campaignPublicId() != null) {
            PromotionCampaign campaign = campaignRepository
                    .findByPublicIdAndDeletedAtIsNull(benefit.campaignPublicId())
                    .orElseThrow(() -> notFound(
                            BenefitErrorCode.CAMPAIGN_NOT_ACTIVE, "Promotion campaign not found"));
            requireActiveCampaign(campaign, now);
            requireCampaignCapacityAvailable(
                    campaign, request.getUserPublicId(),
                    validation.getDiscountAmount(), now);
        }
        return validation;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> history(
            RedemptionType type,
            ReservationStatus status,
            String userPublicId,
            String bookingPublicId,
            String orderPublicId,
            Instant from,
            Instant to,
            int page,
            int size) {
        PageRequest pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PromotionReservation> result = reservationRepository.findAll(
                ReservationSpecifications.filter(
                        type, status, userPublicId, bookingPublicId, orderPublicId, from, to),
                pageable);
        List<ReservationResponse> content = result.getContent().stream()
                .map(reservation -> response(reservation, null))
                .toList();
        return new PagedResponse<>(
                content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override
    @Auditable(action = "EXPIRE_DUE", entityType = "PROMOTION_RESERVATION")
    @CacheEvict(cacheNames = "reservations", allEntries = true)
    public int expireDueReservations(String actor) {
        Instant now = databaseInstant();
        List<String> due = reservationRepository.findDueCandidateIds(
                ReservationStatus.ACTIVE, now, PageRequest.of(0, EXPIRATION_BATCH_SIZE));
        int expiredCount = 0;
        for (String reservationPublicId : due) {
            try {
                Boolean expired = requiresNewTransaction.execute(status -> {
                    PromotionReservation reservation = reservationRepository
                            .findByPublicIdForUpdate(reservationPublicId)
                            .orElse(null);
                    if (reservation == null
                            || reservation.getStatus() != ReservationStatus.ACTIVE
                            || now.isBefore(reservation.getReservationExpiredAt())) {
                        return false;
                    }
                    expireAndRecord(reservation, now, actor);
                    return true;
                });
                if (Boolean.TRUE.equals(expired)) {
                    expiredCount++;
                    metricsManager.incrementReservationExpiration("expired");
                }
            } catch (RuntimeException expirationFailure) {
                metricsManager.incrementReservationExpiration("retry");
                recordExpirationFailure(reservationPublicId, expirationFailure, now, actor);
            }
        }
        return expiredCount;
    }

    private void recordExpirationFailure(
            String reservationPublicId,
            RuntimeException failure,
            Instant now,
            String actor) {
        try {
            requiresNewTransaction.executeWithoutResult(status -> {
                PromotionReservation reservation = reservationRepository
                        .findByPublicIdForUpdate(reservationPublicId)
                        .orElse(null);
                if (reservation == null || reservation.getStatus() != ReservationStatus.ACTIVE) {
                    return;
                }
                int attempts = reservation.getExpirationAttempts() == null
                        ? 1 : reservation.getExpirationAttempts() + 1;
                long retryDelaySeconds = Math.min(900L, 5L * (1L << Math.min(attempts, 8)));
                reservation.setExpirationAttempts(attempts);
                reservation.setExpirationLastAttemptAt(now);
                reservation.setExpirationNextAttemptAt(now.plusSeconds(retryDelaySeconds));
                reservation.setExpirationError(limit(rootMessage(failure), 1000));
                reservation.setUpdatedBy(actor);
                reservationRepository.save(reservation);
            });
        } catch (RuntimeException recordingFailure) {
            // The next scheduler pass will retry the original ACTIVE row.
            log.error("Unable to record expiration failure for reservation {}",
                    reservationPublicId, recordingFailure);
        }
    }

    private String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }

    private String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }

    private LockedBenefit findBenefit(ReserveRequest request) {
        return findBenefit(request.getBenefitType(), request.getCode());
    }

    private LockedBenefit findBenefit(RedemptionType type, String code) {
        if (type == RedemptionType.COUPON) {
            Coupon coupon = couponRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(code)
                    .orElseThrow(() -> notFound(BenefitErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
            return new LockedBenefit(
                    RedemptionType.COUPON, coupon.getPublicId(),
                    coupon.getCampaignPublicId(), coupon.getCode());
        }
        Voucher voucher = voucherRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(code)
                .orElseThrow(() -> notFound(BenefitErrorCode.VOUCHER_NOT_FOUND, "Voucher not found"));
        return new LockedBenefit(
                RedemptionType.VOUCHER, voucher.getPublicId(),
                voucher.getCampaignPublicId(), voucher.getCode());
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

    private void requireBenefitCapacityAvailable(
            LockedBenefit benefit,
            RuntimeValidationRequest request,
            Instant now) {
        requireBenefitCapacityAvailable(
                benefit, request.getUserPublicId(), request.getCustomerPhone(), now);
    }

    private void requireBenefitCapacityAvailable(
            LockedBenefit benefit,
            ReserveRequest request,
            Instant now) {
        requireBenefitCapacityAvailable(
                benefit, request.getUserPublicId(), request.getCustomerPhone(), now);
    }

    private void requireBenefitCapacityAvailable(
            LockedBenefit benefit,
            String userPublicId,
            String customerPhone,
            Instant now) {
        if (benefit.type() == RedemptionType.COUPON) {
            requireCouponCapacityAvailable(benefit.publicId(), userPublicId, customerPhone, now);
            return;
        }
        Voucher voucher = voucherRepository.findByPublicIdAndDeletedAtIsNull(benefit.publicId())
                .orElseThrow(() -> notFound(BenefitErrorCode.VOUCHER_NOT_FOUND, "Voucher not found"));
        long activeReservations = reservationRepository
                .countByVoucherPublicIdAndStatusAndReservationExpiredAtAfterAndDeletedAtIsNull(
                        benefit.publicId(), ReservationStatus.ACTIVE, now);
        if (voucher.getUsageCount() + activeReservations >= voucher.getMaxUsage()) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Voucher usage capacity is fully reserved");
        }
    }

    private void requireCouponCapacityAvailable(
            String couponPublicId,
            String userPublicId,
            String customerPhone,
            Instant now) {
        Coupon coupon = couponRepository.findByPublicIdAndDeletedAtIsNull(couponPublicId)
                .orElseThrow(() -> notFound(BenefitErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
        long activeReservations = reservationRepository
                .countByCouponPublicIdAndStatusAndReservationExpiredAtAfterAndDeletedAtIsNull(
                        couponPublicId, ReservationStatus.ACTIVE, now);
        if (coupon.getMaxRedemptions() != null
                && coupon.getRedemptionCount() + activeReservations >= coupon.getMaxRedemptions()) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Coupon usage capacity is fully reserved");
        }

        long activeUserReservations = reservationRepository
                .countByCouponPublicIdAndUserPublicIdAndStatusAndReservationExpiredAtAfterAndDeletedAtIsNull(
                        couponPublicId, userPublicId, ReservationStatus.ACTIVE, now);
        long userUsage = couponRedemptionRepository
                .countByCouponPublicIdAndUserPublicIdAndStatusIn(
                        couponPublicId, userPublicId, CONSUMED_STATUSES);
        if (userUsage + activeUserReservations >= coupon.getMaxRedemptionsPerUser()) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Coupon capacity for this user is fully reserved");
        }

        String normalizedPhone = blankToNull(customerPhone);
        if (normalizedPhone == null) {
            return;
        }
        long activePhoneReservations = reservationRepository
                .countByCouponPublicIdAndCustomerPhoneAndStatusAndReservationExpiredAtAfterAndDeletedAtIsNull(
                        couponPublicId, normalizedPhone, ReservationStatus.ACTIVE, now);
        long phoneUsage = couponRedemptionRepository
                .countByCouponPublicIdAndCustomerPhoneAndStatusIn(
                        couponPublicId, normalizedPhone, CONSUMED_STATUSES);
        if (phoneUsage + activePhoneReservations >= coupon.getMaxRedemptionsPerUser()) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Coupon capacity for this verified phone is fully reserved");
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
        requireCampaignCapacityAvailable(campaign, userPublicId, discountAmount, now);
        if (campaign.getBudgetAmount() != null && campaign.getBudgetAmount().signum() > 0) {
            campaign.setBudgetReserved(campaign.getBudgetReserved().add(discountAmount));
            pauseAtSafetyThreshold(campaign);
            campaign.setUpdatedBy(actor);
            campaignRepository.save(campaign);
        }
    }

    private void requireCampaignCapacityAvailable(
            PromotionCampaign campaign,
            String userPublicId,
            BigDecimal discountAmount,
            Instant now) {
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
        }
    }

    private void requireActiveCampaign(PromotionCampaign campaign, Instant now) {
        if (campaign.getStatus() != CampaignStatus.ACTIVE
                || campaign.getLegalStatus() != LegalStatus.PASSED
                || Boolean.TRUE.equals(campaign.getKillSwitch())
                || campaign.getBudgetAmount() == null
                || campaign.getBudgetAmount().signum() <= 0
                || now.isBefore(campaign.getStartAt())
                || !now.isBefore(campaign.getEndAt())) {
            throw new BusinessException(
                    BenefitErrorCode.CAMPAIGN_NOT_ACTIVE,
                    "Promotion campaign is not active",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void pauseAtSafetyThreshold(PromotionCampaign campaign) {
        if (!Boolean.TRUE.equals(campaign.getAutoPauseWhenBudgetExceeded())
                || campaign.getBudgetAmount() == null
                || campaign.getBudgetAmount().signum() <= 0) {
            return;
        }
        BigDecimal exposure = campaign.getBudgetUsed().add(campaign.getBudgetReserved());
        BigDecimal threshold = campaign.getBudgetAmount()
                .multiply(new BigDecimal("0.98"));
        if (exposure.compareTo(threshold) >= 0) {
            campaign.setStatus(CampaignStatus.PAUSED);
        }
    }

    private Instant maximumRefreshDeadline(PromotionReservation reservation) {
        Instant deadline = reservation.getReservationStartedAt()
                .plusSeconds(MAX_RESERVATION_LIFETIME_SECONDS);
        if (reservation.getReservationType() == RedemptionType.COUPON) {
            Instant benefitDeadline = couponRepository
                    .findByPublicIdAndDeletedAtIsNull(reservation.getCouponPublicId())
                    .map(Coupon::getValidTo)
                    .orElseThrow(() -> notFound(
                            BenefitErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
            deadline = earlier(deadline, benefitDeadline);
        } else {
            Instant benefitDeadline = voucherRepository
                    .findByPublicIdAndDeletedAtIsNull(reservation.getVoucherPublicId())
                    .map(Voucher::getValidTo)
                    .orElseThrow(() -> notFound(
                            BenefitErrorCode.VOUCHER_NOT_FOUND, "Voucher not found"));
            deadline = earlier(deadline, benefitDeadline);
        }
        if (reservation.getCampaignPublicId() != null) {
            Instant campaignDeadline = campaignRepository
                    .findByPublicIdAndDeletedAtIsNull(reservation.getCampaignPublicId())
                    .map(PromotionCampaign::getEndAt)
                    .orElseThrow(() -> notFound(
                            BenefitErrorCode.CAMPAIGN_NOT_ACTIVE, "Promotion campaign not found"));
            deadline = earlier(deadline, campaignDeadline);
        }
        return deadline;
    }

    private void validateReservationSnapshot(PromotionReservation reservation) {
        ReservedRedemptionRequest request = redeemRequest(reservation, null);
        ValidationResponse current = reservation.getReservationType() == RedemptionType.COUPON
                ? redemptionService.validateCoupon(request)
                : redemptionService.validateVoucher(request);
        boolean samePricing = money(current.getOriginalAmount())
                .compareTo(money(reservation.getOriginalAmount())) == 0
                && money(current.getDiscountAmount())
                .compareTo(money(reservation.getDiscountAmount())) == 0
                && money(current.getFinalAmount())
                .compareTo(money(reservation.getFinalAmount())) == 0
                && Objects.equals(current.getCurrency(), reservation.getCurrency());
        if (!samePricing) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Promotion pricing changed; create a new reservation");
        }
    }

    private void requireSameReason(String existingReason, String requestedReason) {
        if (!Objects.equals(existingReason, requestedReason)) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                    "Reservation transition was already completed with another reason");
        }
    }

    private Instant earlier(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private void expire(PromotionReservation reservation, Instant now, String actor) {
        releaseCampaignCapacity(reservation, actor);
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservation.setReservationScopeKey(null);
        reservation.setExpirationError(null);
        reservation.setExpirationNextAttemptAt(null);
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);
        evictCapacityCachesAfterCommit();
    }

    private ReservationResponse expireAndRecord(
            PromotionReservation reservation, Instant now, String actor) {
        expire(reservation, now, actor);
        ReservationResponse response = response(reservation, null);
        eventService.record(
                "PROMOTION_RESERVATION", reservation.getPublicId(),
                "RESERVATION_EXPIRED", response, actor);
        return response;
    }

    private Instant databaseInstant() {
        return databaseTimeProvider.now();
    }

    private void evictCapacityCachesAfterCommit() {
        Runnable eviction = () -> {
            clearCache("promotions");
            clearCache("vouchers");
            clearCache("reservations");
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eviction.run();
                        }
                    });
            return;
        }
        eviction.run();
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
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
            if (campaign.getBudgetReserved().compareTo(reservation.getDiscountAmount()) < 0) {
                throw conflict(
                        ReservationErrorCode.RESERVATION_CONFLICT,
                        "Reserved campaign budget is inconsistent");
            }
            campaign.setBudgetReserved(
                    campaign.getBudgetReserved().subtract(reservation.getDiscountAmount()));
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

    private ReservedRedemptionRequest redeemRequest(
            PromotionReservation reservation, String paymentPublicId) {
        ReservedRedemptionRequest request = new ReservedRedemptionRequest();
        request.setCode(benefitCode(reservation));
        request.setUserPublicId(reservation.getUserPublicId());
        request.setOriginalAmount(reservation.getOriginalAmount());
        request.setDiscountAmount(reservation.getDiscountAmount());
        request.setFinalAmount(reservation.getFinalAmount());
        request.setCurrency(reservation.getCurrency());
        request.setReservationPublicId(reservation.getPublicId());
        request.setBookingPublicId(reservation.getBookingPublicId());
        request.setOrderPublicId(reservation.getOrderPublicId());
        request.setPaymentPublicId(paymentPublicId);
        JsonNode metadata = readJson(reservation.getMetadataJson());
        request.setCustomerPhone(reservationPhone(reservation, metadata));
        JsonNode context = metadata == null ? null : metadata.get("context");
        request.setContextJson(context);
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
        JsonNode metadata = readJson(reservation.getMetadataJson());
        boolean samePayload = Objects.equals(reservation.getUserPublicId(), request.getUserPublicId())
                && Objects.equals(
                        reservationPhone(reservation, metadata),
                        blankToNull(request.getCustomerPhone()))
                && money(reservation.getOriginalAmount()).compareTo(money(request.getOriginalAmount())) == 0
                && Objects.equals(reservation.getBookingPublicId(), request.getBookingPublicId())
                && Objects.equals(reservation.getOrderPublicId(), request.getOrderPublicId())
                && Objects.equals(reservation.getPaymentPublicId(), request.getPaymentPublicId())
                && reservation.getReservationExpiredAt().getEpochSecond()
                - reservation.getReservationStartedAt().getEpochSecond()
                == request.getHoldDurationSeconds()
                && sameMetadata(metadata, request);
        if (!sameBenefit || !samePayload) {
            throw conflict(
                    ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                    "Idempotency key was already used for another reservation request");
        }
    }

    private boolean sameMetadata(JsonNode metadata, ReserveRequest request) {
        JsonNode existingContext = metadata == null ? null : metadata.get("context");
        return Objects.equals(existingContext, request.getContextJson());
    }

    private String reservationPhone(PromotionReservation reservation, JsonNode metadata) {
        String phone = blankToNull(reservation.getCustomerPhone());
        if (phone == null && metadata != null && metadata.hasNonNull("customerPhone")) {
            phone = blankToNull(metadata.get("customerPhone").asText());
        }
        return phone;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String metadata(ReserveRequest request) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("schemaVersion", 2);
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

    private String reservationScopeKey(ReserveRequest request) {
        if (request.getOrderPublicId() != null && !request.getOrderPublicId().isBlank()) {
            return "ORDER:" + request.getOrderPublicId().trim();
        }
        return "BOOKING:" + request.getBookingPublicId().trim();
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

    private String reservationCode(String actor, String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((actor + ":" + idempotencyKey.trim())
                            .getBytes(StandardCharsets.UTF_8));
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

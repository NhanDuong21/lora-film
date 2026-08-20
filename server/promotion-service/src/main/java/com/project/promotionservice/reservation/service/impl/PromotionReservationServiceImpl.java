package com.project.promotionservice.reservation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.promotion.service.PromotionCatalogEventService;
import com.project.promotionservice.common.audit.Auditable;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.monitoring.PromotionMetricsManager;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.common.time.DatabaseTimeProvider;
import com.project.promotionservice.configuration.domain.ConfigurationService;
import com.project.promotionservice.promotion.dto.request.PromotionCheckoutRequest;
import com.project.promotionservice.promotion.dto.response.AppliedPromotionResponse;
import com.project.promotionservice.promotion.dto.response.PromotionCheckoutResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.PromotionRedemption;
import com.project.promotionservice.promotion.entity.PromotionRedemptionAdjustment;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionAdjustmentRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import com.project.promotionservice.promotion.service.PromotionEngineService;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.CompensateRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RefreshRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.entity.PromotionReservation;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.exception.ReservationErrorCode;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import com.project.promotionservice.reservation.service.ReservationLockManager;
import com.project.promotionservice.reservation.specification.ReservationSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PromotionReservationServiceImpl implements PromotionReservationService {

    private static final Logger log =
            LoggerFactory.getLogger(PromotionReservationServiceImpl.class);
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;
    private static final int MAX_RESERVATION_LIFETIME_SECONDS = 1800;
    private static final int EXPIRATION_BATCH_SIZE = 100;
    private static final EnumSet<ReservationStatus> FINALIZED_BUSINESS_KEY_STATUSES =
            EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.REVERSED);

    private final PromotionReservationRepository reservationRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final PromotionRedemptionAdjustmentRepository adjustmentRepository;
    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository walletRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionEngineService engineService;
    private final ReservationLockManager lockManager;
    private final ConfigurationService configurationService;
    private final DatabaseTimeProvider databaseTimeProvider;
    private final PromotionCatalogEventService eventService;
    private final PromotionMetricsManager metricsManager;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate requiresNewTransaction;

    public PromotionReservationServiceImpl(
            PromotionReservationRepository reservationRepository,
            PromotionRedemptionRepository redemptionRepository,
            PromotionRedemptionAdjustmentRepository adjustmentRepository,
            PromotionRepository promotionRepository,
            UserPromotionRepository walletRepository,
            PromotionCampaignRepository campaignRepository,
            PromotionEngineService engineService,
            ReservationLockManager lockManager,
            ConfigurationService configurationService,
            DatabaseTimeProvider databaseTimeProvider,
            PromotionCatalogEventService eventService,
            PromotionMetricsManager metricsManager,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.reservationRepository = reservationRepository;
        this.redemptionRepository = redemptionRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.promotionRepository = promotionRepository;
        this.walletRepository = walletRepository;
        this.campaignRepository = campaignRepository;
        this.engineService = engineService;
        this.lockManager = lockManager;
        this.configurationService = configurationService;
        this.databaseTimeProvider = databaseTimeProvider;
        this.eventService = eventService;
        this.metricsManager = metricsManager;
        this.objectMapper = objectMapper;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionCheckoutResponse preview(PromotionCheckoutRequest request) {
        return engineService.preview(request);
    }

    @Override
    @Transactional
    @Auditable(action = "RESERVE", entityType = "PROMOTION_RESERVATION")
    public ReservationResponse reserve(
            ReserveRequest request, String idempotencyKey, String actor) {
        requireIdempotencyKey(idempotencyKey);
        lockManager.lockCheckout(request.orderPublicId(), request.bookingPublicId());
        Instant now = databaseTimeProvider.now();
        String scopeKey = reservationScopeKey(request);
        String code = reservationCode(actor, idempotencyKey);

        PromotionReservation replay = reservationRepository
                .findByReservationCodeAndDeletedAtIsNull(code).orElse(null);
        if (replay != null) {
            assertSameRequest(replay, request);
            return response(replay);
        }
        PromotionReservation scoped = reservationRepository
                .findByReservationScopeKeyAndDeletedAtIsNull(scopeKey).orElse(null);
        if (scoped != null) {
            if (scoped.getStatus() == ReservationStatus.ACTIVE
                    && !now.isBefore(scoped.getReservationExpiredAt())) {
                scoped = reservationRepository.findByPublicIdForUpdate(scoped.getPublicId())
                        .orElseThrow(() -> notFound("Reservation was not found"));
                expire(scoped, now, actor);
            } else {
                throw conflict("Checkout already has an effective promotion reservation");
            }
        }
        if (hasFinalizedBusinessKey(request)) {
            throw conflict(
                    "Checkout business key already belongs to a confirmed or reversed promotion reservation");
        }

        PromotionCheckoutResponse advisory = engineService.preview(request.checkoutRequest());
        if (!advisory.eligible() || advisory.appliedPromotions().isEmpty()) {
            throw new BusinessException(
                    "PROMOTION_NOT_ELIGIBLE",
                    "No eligible promotion can be reserved for this checkout",
                    HttpStatus.BAD_REQUEST);
        }
        lockSelection(advisory, request.userPublicId(), actor);
        PromotionCheckoutResponse authoritative =
                engineService.preview(request.checkoutRequest());
        if (!samePromotionSet(advisory, authoritative)) {
            lockSelection(authoritative, request.userPublicId(), actor);
            authoritative = engineService.preview(request.checkoutRequest());
        }
        if (!authoritative.eligible() || authoritative.appliedPromotions().isEmpty()) {
            throw conflict("Promotion selection changed while reserving capacity");
        }

        reserveCampaignBudgets(authoritative.appliedPromotions(), actor);
        int holdSeconds = effectiveHoldDurationSeconds(request.holdDurationSeconds());
        PromotionReservation reservation = new PromotionReservation();
        reservation.setReservationCode(code);
        reservation.setBookingPublicId(blankToNull(request.bookingPublicId()));
        reservation.setOrderPublicId(blankToNull(request.orderPublicId()));
        reservation.setUserPublicId(request.userPublicId());
        reservation.setCustomerPhone(blankToNull(request.customerPhone()));
        reservation.setReservationScopeKey(scopeKey);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setOriginalAmount(authoritative.originalAmount());
        reservation.setDiscountAmount(authoritative.discountAmount());
        reservation.setFinalAmount(authoritative.finalAmount());
        reservation.setCurrency(authoritative.currency());
        reservation.setReservationStartedAt(now);
        reservation.setReservationExpiredAt(now.plusSeconds(holdSeconds));
        reservation.setMetadataJson(writeNullable(request.contextJson()));
        reservation.setCreatedBy(actor);
        reservation.setUpdatedBy(actor);
        PromotionReservation saved = reservationRepository.save(reservation);

        BigDecimal amountBefore = saved.getOriginalAmount();
        int sequence = 1;
        for (AppliedPromotionResponse applied : authoritative.appliedPromotions()) {
            Promotion template = requirePromotionForUpdate(applied.promotionPublicId());
            PromotionRedemption redemption = new PromotionRedemption();
            redemption.setReservationPublicId(saved.getPublicId());
            redemption.setUserPublicId(saved.getUserPublicId());
            redemption.setCustomerPhone(saved.getCustomerPhone());
            redemption.setPromotionPublicId(applied.promotionPublicId());
            redemption.setCampaignPublicId(applied.campaignPublicId());
            redemption.setPromotionType(applied.promotionType());
            redemption.setPromotionCode(applied.code());
            redemption.setPromotionName(applied.name());
            redemption.setPromotionPriority(applied.priority());
            redemption.setPromotionStackable(applied.stackable());
            redemption.setConditionsSnapshotJson(template.getConditionsJson());
            redemption.setActionsSnapshotJson(template.getActionsJson());
            redemption.setSequenceNo(sequence++);
            redemption.setUserPromotionPublicId(applied.userPromotionPublicId());
            redemption.setBookingPublicId(saved.getBookingPublicId());
            redemption.setOrderPublicId(saved.getOrderPublicId());
            redemption.setStatus(PromotionRedemptionStatus.RESERVED);
            redemption.setDiscountAmount(applied.discountAmount());
            redemption.setOriginalAmount(amountBefore);
            amountBefore = money(amountBefore.subtract(applied.discountAmount()));
            redemption.setFinalAmount(amountBefore);
            redemption.setMetadataJson(saved.getMetadataJson());
            redemption.setCreatedBy(actor);
            redemption.setUpdatedBy(actor);
            redemptionRepository.save(redemption);
        }

        ReservationResponse response = response(saved);
        eventService.record("PROMOTION_RESERVATION", saved.getPublicId(),
                "RESERVATION_CREATED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "CONFIRM", entityType = "PROMOTION_RESERVATION")
    public ReservationResponse confirm(
            String reservationPublicId, ConfirmRequest request,
            String idempotencyKey, String actor) {
        requireIdempotencyKey(idempotencyKey);
        lockManager.lockReservation(reservationPublicId);
        PromotionReservation reservation = requireForUpdate(reservationPublicId);
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            if (!Objects.equals(reservation.getPaymentPublicId(), request.paymentPublicId())) {
                throw conflict("Reservation was confirmed by another payment");
            }
            return response(reservation);
        }
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw conflict("Only an active reservation can be confirmed");
        }
        Instant now = databaseTimeProvider.now();
        if (!now.isBefore(reservation.getReservationExpiredAt())) {
            expire(reservation, now, actor);
            return response(reservation);
        }

        List<PromotionRedemption> redemptions = lockRedemptionResources(
                reservationPublicId);
        if (redemptions.isEmpty()) {
            throw conflict("Active reservation has no promotion redemptions");
        }
        redemptions.stream().map(PromotionRedemption::getCampaignPublicId)
                .distinct().forEach(campaignPublicId -> {
                    if (redemptionRepository
                            .existsConfirmedCampaignConsumptionForBusinessKey(
                                    campaignPublicId, reservationPublicId,
                                    reservation.getBookingPublicId(),
                                    reservation.getOrderPublicId())) {
                        throw conflict("Campaign was already consumed by this booking/order");
                    }
                });
        Map<String, CampaignConsumption> campaignConsumption = new LinkedHashMap<>();
        for (PromotionRedemption redemption : redemptions) {
            if (redemption.getStatus() != PromotionRedemptionStatus.RESERVED) {
                throw conflict("Reservation redemption is not in RESERVED state");
            }
            Promotion promotion = requirePromotionForUpdate(
                    redemption.getPromotionPublicId());
            promotion.setRedemptionCount(promotion.getRedemptionCount() + 1);
            promotion.setUpdatedBy(actor);
            promotionRepository.save(promotion);

            campaignConsumption.computeIfAbsent(
                    redemption.getCampaignPublicId(), ignored -> new CampaignConsumption())
                    .add(redemption.getDiscountAmount());
            consumeWallet(redemption, actor);
            redemption.setStatus(PromotionRedemptionStatus.CONFIRMED);
            redemption.setPaymentPublicId(request.paymentPublicId());
            redemption.setConfirmedAt(now);
            redemption.setUpdatedBy(actor);
            redemptionRepository.save(redemption);
        }
        consumeCampaignBudgets(campaignConsumption, actor);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentPublicId(request.paymentPublicId());
        reservation.setConfirmedAt(now);
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);
        ReservationResponse response = response(reservation);
        eventService.record("PROMOTION_RESERVATION", reservation.getPublicId(),
                "RESERVATION_CONFIRMED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "RELEASE", entityType = "PROMOTION_RESERVATION")
    public ReservationResponse release(
            String reservationPublicId, TransitionRequest request,
            String idempotencyKey, String actor) {
        requireIdempotencyKey(idempotencyKey);
        lockManager.lockReservation(reservationPublicId);
        PromotionReservation reservation = requireForUpdate(reservationPublicId);
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            if (reservation.getReleaseReasonType() != request.resolvedReasonType()
                    || !Objects.equals(reservation.getReasonDetail(), request.resolvedReasonDetail())) {
                throw conflict("Reservation was released with another reason");
            }
            return response(reservation);
        }
        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            return response(reservation);
        }
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw conflict("Confirmed reservation cannot be released");
        }
        Instant now = databaseTimeProvider.now();
        String reasonDetail = request.resolvedReasonDetail();
        rollbackRedemptions(reservation, now, reasonDetail, actor);
        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReservationScopeKey(null);
        reservation.setRollbackAt(now);
        reservation.setRollbackReason(reasonDetail);
        reservation.setReleaseReasonType(request.resolvedReasonType());
        reservation.setReleasedAt(now);
        reservation.setReleasedBy(actor);
        reservation.setSourceService(request.resolvedSourceService(actor));
        reservation.setSourceReference(request.sourceReference());
        reservation.setReasonDetail(reasonDetail);
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);
        ReservationResponse response = response(reservation);
        eventService.record("PROMOTION_RESERVATION", reservation.getPublicId(),
                "RESERVATION_RELEASED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "REVERSE", entityType = "PROMOTION_RESERVATION")
    public ReservationResponse reverseConfirmed(
            String reservationPublicId, CompensateRequest request,
            String idempotencyKey, String actor) {
        requireIdempotencyKey(idempotencyKey);
        lockManager.lockReservation(reservationPublicId);
        PromotionReservation reservation = requireForUpdate(reservationPublicId);
        if (reservation.getStatus() == ReservationStatus.REVERSED) {
            if (!Objects.equals(reservation.getRollbackReason(), request.reason())) {
                throw conflict("Reservation was reversed with another reason");
            }
            return response(reservation);
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw conflict("Only a confirmed reservation can be reversed");
        }

        Instant now = databaseTimeProvider.now();
        List<PromotionRedemption> redemptions = lockRedemptionResources(
                reservationPublicId);
        if (redemptions.isEmpty()) {
            throw conflict("Confirmed reservation has no promotion redemptions");
        }
        Map<String, CampaignConsumption> restoration = new LinkedHashMap<>();
        for (PromotionRedemption redemption : redemptions) {
            if (redemption.getStatus() != PromotionRedemptionStatus.CONFIRMED) {
                throw conflict("Reservation redemption is not in CONFIRMED state");
            }
            Promotion promotion = requirePromotionForUpdate(
                    redemption.getPromotionPublicId());
            if (promotion.getRedemptionCount() <= 0) {
                throw conflict("Promotion redemption count is inconsistent");
            }
            promotion.setRedemptionCount(promotion.getRedemptionCount() - 1);
            promotion.setUpdatedBy(actor);
            promotionRepository.save(promotion);

            restoreWallet(redemption, now, actor);
            restoration.computeIfAbsent(
                    redemption.getCampaignPublicId(), ignored -> new CampaignConsumption())
                    .add(redemption.getDiscountAmount());
            redemption.setStatus(PromotionRedemptionStatus.REVERSED);
            redemption.setRollbackAt(now);
            redemption.setRollbackReason(request.reasonCode() + ": " + request.reason());
            redemption.setUpdatedBy(actor);
            redemptionRepository.save(redemption);

            PromotionRedemptionAdjustment adjustment =
                    new PromotionRedemptionAdjustment();
            adjustment.setRedemptionPublicId(redemption.getPublicId());
            adjustment.setReservationPublicId(reservationPublicId);
            adjustment.setAdjustmentType("REVERSE");
            adjustment.setDiscountAmount(redemption.getDiscountAmount());
            adjustment.setReasonCode(request.reasonCode());
            adjustment.setReason(request.reason());
            adjustment.setOccurredAt(now);
            adjustment.setCreatedBy(actor);
            adjustment.setUpdatedBy(actor);
            adjustmentRepository.save(adjustment);
        }
        restoreCampaignBudgets(restoration, actor);

        reservation.setStatus(ReservationStatus.REVERSED);
        reservation.setRollbackAt(now);
        reservation.setRollbackReason(request.reason());
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);
        ReservationResponse response = response(reservation);
        eventService.record("PROMOTION_RESERVATION", reservation.getPublicId(),
                "RESERVATION_REVERSED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "REFRESH", entityType = "PROMOTION_RESERVATION")
    public ReservationResponse refresh(
            String reservationPublicId, RefreshRequest request,
            String idempotencyKey, String actor) {
        requireIdempotencyKey(idempotencyKey);
        lockManager.lockReservation(reservationPublicId);
        PromotionReservation reservation = requireForUpdate(reservationPublicId);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw conflict("Only an active reservation can be refreshed");
        }
        Instant now = databaseTimeProvider.now();
        if (!now.isBefore(reservation.getReservationExpiredAt())) {
            expire(reservation, now, actor);
            return response(reservation);
        }
        Instant requested = request.requestedExpiredAt()
                .truncatedTo(ChronoUnit.MICROS);
        if (!requested.isAfter(reservation.getReservationExpiredAt())) {
            throw invalid("requestedExpiredAt must extend the current expiration time");
        }
        Instant maximum = maximumRefreshDeadline(reservation);
        if (requested.isAfter(maximum)) {
            throw invalid("Reservation cannot exceed promotion validity or maximum lifetime");
        }
        reservation.setReservationExpiredAt(requested);
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);
        ReservationResponse response = response(reservation);
        eventService.record("PROMOTION_RESERVATION", reservation.getPublicId(),
                "RESERVATION_REFRESHED", response, actor);
        return response;
    }

    @Override
    @Transactional
    public ReservationResponse getDetail(String reservationPublicId, String actor) {
        PromotionReservation reservation = reservationRepository
                .findByPublicIdAndDeletedAtIsNull(reservationPublicId)
                .orElseThrow(() -> notFound("Reservation was not found"));
        Instant now = databaseTimeProvider.now();
        if (reservation.getStatus() == ReservationStatus.ACTIVE
                && !now.isBefore(reservation.getReservationExpiredAt())) {
            reservation = requireForUpdate(reservationPublicId);
            if (reservation.getStatus() == ReservationStatus.ACTIVE
                    && !now.isBefore(reservation.getReservationExpiredAt())) {
                expire(reservation, now, actor);
            }
        }
        return response(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> history(
            ReservationStatus status,
            String userPublicId,
            String bookingPublicId,
            String orderPublicId,
            Instant from,
            Instant to,
            int page,
            int size) {
        return history(status, userPublicId, bookingPublicId, orderPublicId,
                from, to, page, size, null);
    }

    @Override
    public PagedResponse<ReservationResponse> history(
            ReservationStatus status,
            String userPublicId,
            String bookingPublicId,
            String orderPublicId,
            Instant from,
            Instant to,
            int page,
            int size,
            java.util.Set<String> allowedCampaignPublicIds) {
        Page<PromotionReservation> result = reservationRepository.findAll(
                ReservationSpecifications.filter(
                        status, userPublicId, bookingPublicId, orderPublicId, from, to)
                        .and(ReservationSpecifications.allowedCampaigns(
                                allowedCampaignPublicIds)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<ReservationResponse> content = result.getContent().stream()
                .map(this::response)
                .toList();
        return new PagedResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override
    public int expireDueReservations(String actor) {
        int expired = 0;
        int processed = 0;
        while (processed < 5_000) {
            Instant now = databaseTimeProvider.now();
            List<String> due = reservationRepository.findDueCandidateIds(
                    ReservationStatus.ACTIVE, now,
                    PageRequest.of(0, EXPIRATION_BATCH_SIZE));
            if (due.isEmpty()) {
                break;
            }
            for (String publicId : due) {
                processed++;
                try {
                    Boolean changed = requiresNewTransaction.execute(status -> {
                        PromotionReservation reservation = reservationRepository
                                .findByPublicIdForUpdate(publicId).orElse(null);
                        if (reservation == null
                                || reservation.getStatus() != ReservationStatus.ACTIVE
                                || now.isBefore(reservation.getReservationExpiredAt())) {
                            return false;
                        }
                        expire(reservation, now, actor);
                        return true;
                    });
                    if (Boolean.TRUE.equals(changed)) {
                        expired++;
                        metricsManager.incrementReservationExpiration("expired");
                    }
                } catch (RuntimeException failure) {
                    metricsManager.incrementReservationExpiration("retry");
                    recordExpirationFailure(publicId, failure, now, actor);
                }
            }
            if (due.size() < EXPIRATION_BATCH_SIZE) {
                break;
            }
        }
        return expired;
    }

    private void lockSelection(
            PromotionCheckoutResponse checkout, String userPublicId, String actor) {
        List<AppliedPromotionResponse> applied = checkout.appliedPromotions().stream()
                .sorted(Comparator.comparing(AppliedPromotionResponse::promotionPublicId))
                .toList();
        for (AppliedPromotionResponse item : applied) {
            lockManager.lockPromotion(item.promotionPublicId());
            requirePromotionForUpdate(item.promotionPublicId());
        }
        applied.stream().map(AppliedPromotionResponse::campaignPublicId)
                .distinct().sorted()
                .forEach(this::requireCampaignForUpdate);
        for (AppliedPromotionResponse item : applied) {
            if (item.userPromotionPublicId() != null) {
                requireWalletForUpdate(item.userPromotionPublicId(), userPublicId);
            } else if (item.promotionType() == PromotionType.COUPON) {
                ensureCouponWallet(item.promotionPublicId(), userPublicId, actor);
            }
        }
    }

    private UserPromotion ensureCouponWallet(
            String promotionPublicId, String userPublicId, String actor) {
        UserPromotion existing = walletRepository
                .findFirstByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNullOrderByIdDesc(
                        userPublicId, promotionPublicId)
                .orElse(null);
        if (existing != null) {
            return requireWalletForUpdate(existing.getPublicId(), userPublicId);
        }
        Promotion promotion = requirePromotionForUpdate(promotionPublicId);
        UserPromotion wallet = new UserPromotion();
        wallet.setUserPublicId(userPublicId);
        wallet.setPromotionPublicId(promotionPublicId);
        wallet.setStatus(UserPromotionStatus.AVAILABLE);
        wallet.setClaimedAt(databaseTimeProvider.now());
        wallet.setValidFrom(promotion.getValidFrom());
        wallet.setValidTo(promotion.getValidTo());
        wallet.setMaxUsage(promotion.getMaxRedemptionsPerUser());
        wallet.setCreatedBy(actor);
        wallet.setUpdatedBy(actor);
        return walletRepository.save(wallet);
    }

    private void reserveCampaignBudgets(
            List<AppliedPromotionResponse> applied, String actor) {
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        for (AppliedPromotionResponse item : applied) {
            amounts.merge(item.campaignPublicId(), item.discountAmount(), BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> entry : amounts.entrySet()) {
            PromotionCampaign campaign = requireCampaignForUpdate(entry.getKey());
            BigDecimal available = campaign.getBudgetRemaining()
                    .subtract(campaign.getBudgetReserved());
            if (available.compareTo(entry.getValue()) < 0) {
                throw conflict("Campaign budget changed while reserving promotions");
            }
            BigDecimal projectedExposure = campaign.getBudgetUsed()
                    .add(campaign.getBudgetReserved())
                    .add(entry.getValue());
            if (Boolean.TRUE.equals(campaign.getAutoPauseWhenBudgetExceeded())
                    && campaign.getBudgetAmount().signum() > 0
                    && projectedExposure.compareTo(campaign.getBudgetAmount()
                    .multiply(new BigDecimal("0.98"))) >= 0) {
                throw conflict("Campaign budget safety threshold was reached");
            }
            campaign.setBudgetReserved(money(
                    campaign.getBudgetReserved().add(entry.getValue())));
            campaign.setUpdatedBy(actor);
            campaignRepository.save(campaign);
        }
    }

    private void consumeCampaignBudgets(
            Map<String, CampaignConsumption> consumption, String actor) {
        for (Map.Entry<String, CampaignConsumption> entry : consumption.entrySet()) {
            PromotionCampaign campaign = requireCampaignForUpdate(entry.getKey());
            CampaignConsumption value = entry.getValue();
            if (campaign.getBudgetReserved().compareTo(value.discount) < 0) {
                throw conflict("Reserved campaign budget is inconsistent");
            }
            campaign.setBudgetReserved(money(
                    campaign.getBudgetReserved().subtract(value.discount)));
            campaign.setBudgetUsed(money(campaign.getBudgetUsed().add(value.discount)));
            campaign.setBudgetRemaining(money(
                    campaign.getBudgetRemaining().subtract(value.discount)
                            .max(BigDecimal.ZERO)));
            campaign.setRedemptionCount(campaign.getRedemptionCount() + value.count);
            campaign.setUpdatedBy(actor);
            campaignRepository.save(campaign);
        }
    }

    private void consumeWallet(PromotionRedemption redemption, String actor) {
        if (redemption.getUserPromotionPublicId() == null) {
            return;
        }
        UserPromotion wallet = requireWalletForUpdate(
                redemption.getUserPromotionPublicId(), redemption.getUserPublicId());
        int usage = wallet.getUsageCount() + 1;
        if (usage > wallet.getMaxUsage()) {
            throw conflict("Wallet promotion usage is inconsistent");
        }
        wallet.setUsageCount(usage);
        if (usage >= wallet.getMaxUsage()) {
            wallet.setStatus(UserPromotionStatus.USED);
        }
        if (Boolean.TRUE.equals(wallet.getRevocationPending())) {
            wallet.setRevocationPending(false);
            eventService.record("USER_PROMOTION", wallet.getPublicId(),
                    "PROMOTION_AUTOMATION_REFUND_ANOMALY",
                    Map.of(
                            "reason", Objects.toString(
                                    wallet.getRevocationReason(), "SOURCE_BOOKING_REFUNDED"),
                            "resolution", "REDEEMED_WHILE_REVOCATION_PENDING"),
                    "SYSTEM");
        }
        wallet.setUpdatedBy(actor);
        walletRepository.save(wallet);
    }

    private void restoreWallet(
            PromotionRedemption redemption, Instant now, String actor) {
        if (redemption.getUserPromotionPublicId() == null) {
            return;
        }
        UserPromotion wallet = requireWalletForUpdate(
                redemption.getUserPromotionPublicId(), redemption.getUserPublicId());
        if (wallet.getUsageCount() <= 0) {
            throw conflict("Wallet promotion usage is inconsistent");
        }
        wallet.setUsageCount(wallet.getUsageCount() - 1);
        if (wallet.getStatus() == UserPromotionStatus.USED) {
            wallet.setStatus(!now.isBefore(wallet.getValidTo())
                    ? UserPromotionStatus.EXPIRED : UserPromotionStatus.AVAILABLE);
        }
        wallet.setUpdatedBy(actor);
        walletRepository.save(wallet);
    }

    private void restoreCampaignBudgets(
            Map<String, CampaignConsumption> restoration, String actor) {
        for (Map.Entry<String, CampaignConsumption> entry : restoration.entrySet()) {
            PromotionCampaign campaign = requireCampaignForUpdate(entry.getKey());
            CampaignConsumption value = entry.getValue();
            if (campaign.getBudgetUsed().compareTo(value.discount) < 0
                    || campaign.getRedemptionCount() < value.count) {
                throw conflict("Campaign redemption ledger is inconsistent");
            }
            campaign.setBudgetUsed(money(
                    campaign.getBudgetUsed().subtract(value.discount)));
            campaign.setBudgetRemaining(money(campaign.getBudgetRemaining()
                    .add(value.discount).min(campaign.getBudgetAmount())));
            campaign.setRedemptionCount(campaign.getRedemptionCount() - value.count);
            campaign.setUpdatedBy(actor);
            campaignRepository.save(campaign);
        }
    }

    private void rollbackRedemptions(
            PromotionReservation reservation,
            Instant now,
            String reason,
            String actor) {
        List<PromotionRedemption> redemptions =
                redemptionRepository.findByReservationPublicIdForUpdate(
                        reservation.getPublicId());
        List<String> walletIds = redemptions.stream()
                .map(PromotionRedemption::getUserPromotionPublicId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, BigDecimal> releaseByCampaign = new LinkedHashMap<>();
        for (PromotionRedemption redemption : redemptions) {
            if (redemption.getStatus() != PromotionRedemptionStatus.RESERVED) {
                continue;
            }
            releaseByCampaign.merge(
                    redemption.getCampaignPublicId(), redemption.getDiscountAmount(),
                    BigDecimal::add);
            redemption.setStatus(PromotionRedemptionStatus.ROLLBACKED);
            redemption.setRollbackAt(now);
            redemption.setRollbackReason(reason);
            redemption.setUpdatedBy(actor);
            redemptionRepository.save(redemption);
        }
        for (Map.Entry<String, BigDecimal> entry : releaseByCampaign.entrySet()) {
            PromotionCampaign campaign = requireCampaignForUpdate(entry.getKey());
            if (campaign.getBudgetReserved().compareTo(entry.getValue()) < 0) {
                throw conflict("Reserved campaign budget is inconsistent");
            }
            campaign.setBudgetReserved(money(
                    campaign.getBudgetReserved().subtract(entry.getValue())));
            campaign.setUpdatedBy(actor);
            campaignRepository.save(campaign);
        }
        for (String walletId : walletIds) {
            UserPromotion wallet = walletRepository.findByPublicIdForUpdate(walletId)
                    .orElse(null);
            if (wallet == null || !Boolean.TRUE.equals(wallet.getRevocationPending())
                    || wallet.getUsageCount() > 0) {
                continue;
            }
            wallet.setStatus(UserPromotionStatus.REVOKED);
            wallet.setRevocationPending(false);
            wallet.setUpdatedBy("SYSTEM");
            walletRepository.save(wallet);
            eventService.record("USER_PROMOTION", wallet.getPublicId(),
                    "PROMOTION_AUTOMATION_REVOKED_AFTER_RESERVATION_RELEASE",
                    Map.of("reason", Objects.toString(
                            wallet.getRevocationReason(), "SOURCE_BOOKING_REFUNDED")),
                    "SYSTEM");
        }
    }

    private List<PromotionRedemption> lockRedemptionResources(String reservationPublicId) {
        List<PromotionRedemption> redemptions =
                redemptionRepository.findByReservationPublicIdForUpdate(reservationPublicId);
        redemptions.stream().map(PromotionRedemption::getPromotionPublicId)
                .distinct().sorted().forEach(this::requirePromotionForUpdate);
        List<String> campaigns = redemptions.stream()
                .map(PromotionRedemption::getCampaignPublicId)
                .distinct().sorted().toList();
        campaigns.forEach(this::requireCampaignForUpdate);
        redemptions.stream().map(PromotionRedemption::getUserPromotionPublicId)
                .filter(Objects::nonNull).distinct().sorted()
                .forEach(publicId -> walletRepository.findByPublicIdForUpdate(publicId)
                        .orElseThrow(() -> conflict("Reserved wallet item is missing")));
        return redemptions;
    }

    private void expire(
            PromotionReservation reservation, Instant now, String actor) {
        rollbackRedemptions(reservation, now, "Reservation expired", actor);
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservation.setReservationScopeKey(null);
        reservation.setExpirationAttempts(0);
        reservation.setExpirationError(null);
        reservation.setExpirationNextAttemptAt(null);
        reservation.setUpdatedBy(actor);
        reservationRepository.save(reservation);
        eventService.record("PROMOTION_RESERVATION", reservation.getPublicId(),
                "RESERVATION_EXPIRED", response(reservation), actor);
    }

    private Instant maximumRefreshDeadline(PromotionReservation reservation) {
        Instant maximum = reservation.getReservationStartedAt()
                .plusSeconds(MAX_RESERVATION_LIFETIME_SECONDS);
        List<PromotionRedemption> redemptions = redemptionRepository
                .findByReservationPublicIdAndDeletedAtIsNull(reservation.getPublicId());
        for (PromotionRedemption redemption : redemptions) {
            Promotion promotion = promotionRepository
                    .findByPublicIdAndDeletedAtIsNull(redemption.getPromotionPublicId())
                    .orElseThrow(() -> conflict("Reserved promotion template is missing"));
            maximum = earlier(maximum, promotion.getValidTo());
            PromotionCampaign campaign = campaignRepository
                    .findByPublicIdAndDeletedAtIsNull(promotion.getCampaignPublicId())
                    .orElseThrow(() -> conflict("Reserved promotion campaign is missing"));
            maximum = earlier(maximum, campaign.getEndAt());
        }
        return maximum;
    }

    private ReservationResponse response(PromotionReservation reservation) {
        List<PromotionRedemption> redemptions = redemptionRepository
                .findByReservationPublicIdAndDeletedAtIsNull(reservation.getPublicId());
        List<AppliedPromotionResponse> applied = redemptions.stream()
                .sorted(Comparator.comparingInt(PromotionRedemption::getSequenceNo))
                .map(this::applied)
                .toList();

        ReservationResponse response = new ReservationResponse();
        response.setPublicId(reservation.getPublicId());
        response.setReservationCode(reservation.getReservationCode());
        response.setStatus(reservation.getStatus());
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
        response.setRollbackAt(reservation.getRollbackAt());
        response.setRollbackReason(reservation.getRollbackReason());
        response.setReleaseReasonType(reservation.getReleaseReasonType());
        response.setReleasedAt(reservation.getReleasedAt());
        response.setReleasedBy(reservation.getReleasedBy());
        response.setSourceService(reservation.getSourceService());
        response.setSourceReference(reservation.getSourceReference());
        response.setReasonDetail(reservation.getReasonDetail());
        response.setContextJson(readNullable(reservation.getMetadataJson()));
        response.setAppliedPromotions(applied);
        return response;
    }

    private AppliedPromotionResponse applied(PromotionRedemption redemption) {
        return new AppliedPromotionResponse(
                redemption.getPromotionPublicId(), redemption.getUserPromotionPublicId(),
                redemption.getCampaignPublicId(), redemption.getPromotionType(),
                redemption.getPromotionCode(), redemption.getPromotionName(),
                redemption.getDiscountAmount(), redemption.getPromotionPriority(),
                Boolean.TRUE.equals(redemption.getPromotionStackable()));
    }

    private void assertSameRequest(
            PromotionReservation reservation, ReserveRequest request) {
        if (!Objects.equals(reservation.getUserPublicId(), request.userPublicId())
                || !Objects.equals(reservation.getBookingPublicId(),
                blankToNull(request.bookingPublicId()))
                || !Objects.equals(reservation.getOrderPublicId(),
                blankToNull(request.orderPublicId()))
                || money(reservation.getOriginalAmount())
                .compareTo(money(request.originalAmount())) != 0) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                    "Idempotency key was used for another checkout request",
                    HttpStatus.CONFLICT);
        }
    }

    private boolean samePromotionSet(
            PromotionCheckoutResponse first, PromotionCheckoutResponse second) {
        List<String> firstIds = first.appliedPromotions().stream()
                .map(AppliedPromotionResponse::promotionPublicId).sorted().toList();
        List<String> secondIds = second.appliedPromotions().stream()
                .map(AppliedPromotionResponse::promotionPublicId).sorted().toList();
        return firstIds.equals(secondIds)
                && first.discountAmount().compareTo(second.discountAmount()) == 0;
    }

    private PromotionReservation requireForUpdate(String publicId) {
        return reservationRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound("Reservation was not found"));
    }

    private Promotion requirePromotionForUpdate(String publicId) {
        return promotionRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound("Promotion was not found"));
    }

    private PromotionCampaign requireCampaignForUpdate(String publicId) {
        return campaignRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound("Promotion campaign was not found"));
    }

    private UserPromotion requireWalletForUpdate(
            String publicId, String userPublicId) {
        UserPromotion wallet = walletRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound("Wallet promotion was not found"));
        if (!Objects.equals(wallet.getUserPublicId(), userPublicId)) {
            throw new BusinessException(
                    "WALLET_PROMOTION_FORBIDDEN",
                    "Wallet promotion belongs to another customer",
                    HttpStatus.FORBIDDEN);
        }
        return wallet;
    }

    private int effectiveHoldDurationSeconds(Integer requested) {
        int requestedSeconds = requested == null ? 900 : requested;
        int configuredMinutes = configurationService.getInt(
                "PROMOTION_RESERVATION_TIMEOUT", 15);
        if (configuredMinutes < 1 || configuredMinutes > 30) {
            configuredMinutes = 15;
        }
        return Math.min(
                Math.max(60, requestedSeconds),
                Math.min(MAX_RESERVATION_LIFETIME_SECONDS, configuredMinutes * 60));
    }

    private String reservationScopeKey(ReserveRequest request) {
        return request.orderPublicId() != null && !request.orderPublicId().isBlank()
                ? "ORDER:" + request.orderPublicId().trim()
                : "BOOKING:" + request.bookingPublicId().trim();
    }

    private boolean hasFinalizedBusinessKey(ReserveRequest request) {
        if (request.orderPublicId() != null && !request.orderPublicId().isBlank()) {
            return reservationRepository
                    .existsByOrderPublicIdAndStatusInAndDeletedAtIsNull(
                            request.orderPublicId().trim(),
                            FINALIZED_BUSINESS_KEY_STATUSES);
        }
        return request.bookingPublicId() != null
                && !request.bookingPublicId().isBlank()
                && reservationRepository
                .existsByBookingPublicIdAndStatusInAndDeletedAtIsNull(
                        request.bookingPublicId().trim(),
                        FINALIZED_BUSINESS_KEY_STATUSES);
    }

    private String reservationCode(String actor, String idempotencyKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((actor + ":" + idempotencyKey.trim())
                            .getBytes(StandardCharsets.UTF_8));
            return "RSV-" + HexFormat.of()
                    .formatHex(digest, 0, 24).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                    "X-Idempotency-Key is required and must not exceed 255 characters",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void recordExpirationFailure(
            String publicId, RuntimeException failure, Instant now, String actor) {
        try {
            requiresNewTransaction.executeWithoutResult(status -> {
                PromotionReservation reservation = reservationRepository
                        .findByPublicIdForUpdate(publicId).orElse(null);
                if (reservation == null
                        || reservation.getStatus() != ReservationStatus.ACTIVE) {
                    return;
                }
                int attempts = reservation.getExpirationAttempts() + 1;
                long delay = Math.min(900L, 5L * (1L << Math.min(attempts, 8)));
                reservation.setExpirationAttempts(attempts);
                reservation.setExpirationLastAttemptAt(now);
                reservation.setExpirationNextAttemptAt(now.plusSeconds(delay));
                reservation.setExpirationError(limit(rootMessage(failure), 1000));
                reservation.setUpdatedBy(actor);
                reservationRepository.save(reservation);
            });
        } catch (RuntimeException recordingFailure) {
            log.error("Unable to record expiration failure for {}",
                    publicId, recordingFailure);
        }
    }

    private String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null
                ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength
                ? value : value.substring(0, maxLength);
    }

    private String writeNullable(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw invalid("Checkout context JSON is invalid");
        }
    }

    private JsonNode readNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw conflict("Reservation context JSON is invalid");
        }
    }

    private Instant earlier(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BusinessException invalid(String message) {
        return new BusinessException(
                ReservationErrorCode.RESERVATION_INVALID_STATE,
                message, HttpStatus.BAD_REQUEST);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(
                ReservationErrorCode.RESERVATION_CONFLICT,
                message, HttpStatus.CONFLICT);
    }

    private BusinessException notFound(String message) {
        return new BusinessException(
                ReservationErrorCode.RESERVATION_NOT_FOUND,
                message, HttpStatus.NOT_FOUND);
    }

    private static final class CampaignConsumption {
        private BigDecimal discount = BigDecimal.ZERO;
        private int count;

        private void add(BigDecimal amount) {
            discount = discount.add(amount);
            count = 1;
        }
    }
}

package com.project.scoreservice.service.impl;

import com.project.scoreservice.client.BookingContext;
import com.project.scoreservice.client.BookingInternalClient;
import com.project.scoreservice.dto.*;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.MembershipTierHistory;
import com.project.scoreservice.entity.PointExpirationBucket;
import com.project.scoreservice.entity.ScoreHistory;
import com.project.scoreservice.entity.ScoreHold;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.enumtype.PointExpirationBucketStatus;
import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreHoldStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.enumtype.UserScoreStatus;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.MembershipTierHistoryRepository;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.repository.PointExpirationBucketRepository;
import com.project.scoreservice.repository.ScoreHistoryRepository;
import com.project.scoreservice.repository.ScoreHoldRepository;
import com.project.scoreservice.repository.UserScoreRepository;
import com.project.scoreservice.service.OutboxService;
import com.project.scoreservice.service.ScoreService;
import com.project.scoreservice.monitoring.ScoreMetricsService;
import java.time.LocalDate;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ScoreServiceImpl implements ScoreService {

    private static final Logger log = LoggerFactory.getLogger(ScoreServiceImpl.class);

    private final UserScoreRepository userScoreRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final ScoreHoldRepository scoreHoldRepository;
    private final PointExpirationBucketRepository pointExpirationBucketRepository;
    private final MembershipTierHistoryRepository membershipTierHistoryRepository;
    private final OutboxService outboxService;
    private final ScoreMetricsService metricsService;
    private final BookingInternalClient bookingInternalClient;
    private final BigDecimal redemptionValuePerPoint;
    private final ScoreService self;

    public ScoreServiceImpl(UserScoreRepository userScoreRepository,
                            MembershipTierRepository membershipTierRepository,
                            ScoreHistoryRepository scoreHistoryRepository,
                            ScoreHoldRepository scoreHoldRepository,
                            PointExpirationBucketRepository pointExpirationBucketRepository,
                            MembershipTierHistoryRepository membershipTierHistoryRepository,
                            OutboxService outboxService,
                            ScoreMetricsService metricsService,
                            BookingInternalClient bookingInternalClient,
                            @Value("${score.redemption.value-per-point:1000}") BigDecimal redemptionValuePerPoint,
                            @Lazy ScoreService self) {
        this.userScoreRepository = userScoreRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.scoreHistoryRepository = scoreHistoryRepository;
        this.scoreHoldRepository = scoreHoldRepository;
        this.pointExpirationBucketRepository = pointExpirationBucketRepository;
        this.membershipTierHistoryRepository = membershipTierHistoryRepository;
        this.outboxService = outboxService;
        this.metricsService = metricsService;
        this.bookingInternalClient = bookingInternalClient;
        if (redemptionValuePerPoint == null || redemptionValuePerPoint.signum() <= 0) {
            throw new IllegalArgumentException("score.redemption.value-per-point must be greater than zero");
        }
        this.redemptionValuePerPoint = redemptionValuePerPoint;
        this.self = self;
    }


    @Override
    @Transactional
    public UserScoreResponse getUserScore(Long userId) {
        UserScore userScore = getOrInitializeUserScore(userId);
        return mapToUserScoreResponse(userScore);
    }

    @Override
    @Transactional
    public InternalUserScoreResponse getInternalUserScore(Long userId) {
        UserScore userScore = getOrInitializeUserScore(userId);
        return new InternalUserScoreResponse(
                userScore.getUserId(),
                userScore.getCurrentPoints(),
                userScore.getHeldPoints(),
                userScore.getAccumulatedPoints(),
                userScore.getCurrentTier().getTierCode(),
                userScore.getCurrentTier().getTierName(),
                userScore.getCurrentTier().getEarningRate(),
                userScore.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScoreHistoryResponse> getUserHistory(Long userId, int page, int size, ScoreTransactionType transactionType, Long bookingId, LocalDateTime from, LocalDateTime to, String sort) {
        Specification<ScoreHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userScore").get("userId"), userId));

            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (bookingId != null) {
                predicates.add(cb.equal(root.get("bookingId"), bookingId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sortOrder = Sort.by(Sort.Direction.DESC, "occurredAt");
        if (sort != null && !sort.isEmpty()) {
            if ("asc".equalsIgnoreCase(sort) || "occurredAt,asc".equalsIgnoreCase(sort)) {
                sortOrder = Sort.by(Sort.Direction.ASC, "occurredAt");
            }
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortOrder);
        Page<ScoreHistory> historyPage = scoreHistoryRepository.findAll(spec, pageable);

        return historyPage.map(this::mapToHistoryResponse);
    }

    private UserScore getOrInitializeUserScore(Long userId) {
        boolean exists = userScoreRepository.existsByUserId(userId);
        if (!exists) {
            try {
                (self != null ? self : this).initializeUserScoreRequiresNew(userId);
            } catch (Exception e) {
                log.warn("Concurrent initialization detected for userId: {}. Fetching existing score with lock.", userId);
            }
        }
        return userScoreRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new BusinessException("Failed to initialize or fetch user score for user: " + userId, "SCORE_INIT_FAILED", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private UserScoreResponse mapToUserScoreResponse(UserScore userScore) {
        MembershipTier currentTier = userScore.getCurrentTier();
        MembershipTierResponse currentTierResponse = new MembershipTierResponse(
                currentTier.getId(),
                currentTier.getTierCode(),
                currentTier.getTierName(),
                currentTier.getMinAccumulatedPoints(),
                currentTier.getEarningRate(),
                currentTier.getPriority()
        );

        Optional<MembershipTier> nextTierOpt = membershipTierRepository
                .findFirstByIsActiveTrueAndMinAccumulatedPointsGreaterThanOrderByMinAccumulatedPointsAsc(userScore.getAccumulatedPoints());

        NextTierResponse nextTierResponse;
        if (nextTierOpt.isPresent()) {
            MembershipTier nextTier = nextTierOpt.get();
            int required = Math.max(0, nextTier.getMinAccumulatedPoints() - userScore.getAccumulatedPoints());
            nextTierResponse = new NextTierResponse(
                    nextTier.getId(),
                    nextTier.getTierCode(),
                    nextTier.getTierName(),
                    nextTier.getMinAccumulatedPoints(),
                    required
            );
        } else {
            nextTierResponse = new NextTierResponse(
                    currentTier.getId(),
                    currentTier.getTierCode(),
                    currentTier.getTierName(),
                    currentTier.getMinAccumulatedPoints(),
                    0
            );
        }

        return new UserScoreResponse(
                userScore.getUserId(),
                userScore.getCurrentPoints(),
                userScore.getHeldPoints(),
                userScore.getAccumulatedPoints(),
                currentTierResponse,
                nextTierResponse,
                userScore.getStatus(),
                userScore.getOutstandingPoints()
        );
    }

    private ScoreHistoryResponse mapToHistoryResponse(ScoreHistory sh) {
        return new ScoreHistoryResponse(
                sh.getId(),
                sh.getEventId(),
                sh.getBookingId(),
                sh.getActualPointChange(),
                sh.getTransactionType(),
                sh.getBalanceBefore(),
                sh.getBalanceAfter(),
                sh.getAccumulatedBefore(),
                sh.getAccumulatedAfter(),
                sh.getReferenceHistory() != null ? sh.getReferenceHistory().getId() : null,
                sh.getDescription(),
                sh.getCreatedAt(),
                sh.getOccurredAt()
        );
    }

    @Override
    @Transactional
    public ScoreEarnResponse earnPoints(ScoreEarnRequest request) {
        if (request.eligibleAmount() == null || request.eligibleAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Eligible amount must be positive", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        // Check idempotency by key
        Optional<ScoreHistory> existingOpt = scoreHistoryRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingOpt.isEmpty() && request.eventId() != null && !request.eventId().isEmpty()) {
            existingOpt = scoreHistoryRepository.findByEventId(request.eventId());
        }
        if (existingOpt.isPresent()) {
            ScoreHistory sh = existingOpt.get();
            if (!sh.getUserScore().getUserId().equals(request.userId()) ||
                (sh.getBookingId() != null && !sh.getBookingId().equals(request.bookingId()))) {
                throw new BusinessException("Idempotency conflict: request payload differs from existing transaction", "SCORE_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT);
            }
            UserScore us = sh.getUserScore();
            return new ScoreEarnResponse(
                    sh.getActualPointChange() != null ? sh.getActualPointChange() : sh.getPointChange(),
                    sh.getBalanceBefore(),
                    sh.getBalanceAfter(),
                    sh.getAccumulatedBefore(),
                    sh.getAccumulatedAfter(),
                    us.getCurrentTier().getTierCode(),
                    us.getCurrentTier().getTierCode(),
                    false,
                    true
            );
        }

        // Check BR-EARN-006: 1 Booking can only earn once
        if (request.bookingId() != null) {
            List<ScoreHistory> byBooking = scoreHistoryRepository.findByBookingId(request.bookingId());
            boolean alreadyEarned = byBooking.stream()
                    .anyMatch(sh -> sh.getTransactionType() == ScoreTransactionType.EARN && (sh.getPointChange() > 0 || (sh.getActualPointChange() != null && sh.getActualPointChange() > 0)));
            if (alreadyEarned) {
                throw new BusinessException("Booking has already earned points", "SCORE_EARN_ALREADY_EXISTS", HttpStatus.CONFLICT);
            }
        }

        UserScore userScore = getOrInitializeUserScore(request.userId());

        BigDecimal earningRate = userScore.getCurrentTier().getEarningRate();
        int earnedPoints = request.eligibleAmount().multiply(earningRate).divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR).intValue();

        if (earnedPoints == 0) {
            return new ScoreEarnResponse(
                    0,
                    userScore.getCurrentPoints(),
                    userScore.getCurrentPoints(),
                    userScore.getAccumulatedPoints(),
                    userScore.getAccumulatedPoints(),
                    userScore.getCurrentTier().getTierCode(),
                    userScore.getCurrentTier().getTierCode(),
                    false,
                    false
            );
        }

        // Check point overflow
        if ((long) userScore.getCurrentPoints() + earnedPoints > Integer.MAX_VALUE) {
            throw new BusinessException("Point overflow: balance would exceed maximum allowed integer", "SCORE_POINT_OVERFLOW", HttpStatus.BAD_REQUEST);
        }

        int oldBalance = userScore.getCurrentPoints();
        int oldAccumulated = userScore.getAccumulatedPoints();
        MembershipTier oldTier = userScore.getCurrentTier();
        String previousTierCode = oldTier.getTierCode();

        userScore.setCurrentPoints(oldBalance + earnedPoints);
        userScore.setAccumulatedPoints(oldAccumulated + earnedPoints);
        userScore.setLastEarnAt(LocalDateTime.now());

        // Check tier upgrade
        boolean tierChanged = false;
        List<MembershipTier> allTiers = membershipTierRepository.findAll();
        MembershipTier newTier = oldTier;
        for (MembershipTier t : allTiers) {
            if (Boolean.TRUE.equals(t.getActive()) && userScore.getAccumulatedPoints() >= t.getMinAccumulatedPoints()) {
                if (t.getMinAccumulatedPoints() >= newTier.getMinAccumulatedPoints()) {
                    newTier = t;
                }
            }
        }
        if (!newTier.getTierCode().equals(previousTierCode)) {
            userScore.setCurrentTier(newTier);
            tierChanged = true;
        }

        userScoreRepository.save(userScore);

        ScoreHistory history = ScoreHistory.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userScore(userScore)
                .bookingId(request.bookingId())
                .eventId(request.eventId())
                .idempotencyKey(request.idempotencyKey())
                .transactionType(ScoreTransactionType.EARN)
                .pointChange(earnedPoints)
                .actualPointChange(earnedPoints)
                .balanceBefore(oldBalance)
                .balanceAfter(userScore.getCurrentPoints())
                .heldBefore(userScore.getHeldPoints())
                .heldAfter(userScore.getHeldPoints())
                .accumulatedBefore(oldAccumulated)
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .outstandingBefore(userScore.getOutstandingPoints())
                .outstandingAfter(userScore.getOutstandingPoints())
                .tierSnapshot(previousTierCode)
                .earningRateSnapshot(earningRate)
                .sourceService("BOOKING")
                .description("Earned points for booking " + request.bookingId())
                .build();
        scoreHistoryRepository.save(history);

        if (earnedPoints > 0) {
            PointExpirationBucket bucket = PointExpirationBucket.builder()
                    .userScore(userScore)
                    .scoreHistory(history)
                    .bookingId(request.bookingId())
                    .earnedPoints(earnedPoints)
                    .remainingPoints(earnedPoints)
                    .expiredPoints(0)
                    .consumedPoints(0)
                    .expirationDate(LocalDate.now().plusYears(1))
                    .status(PointExpirationBucketStatus.ACTIVE)
                    .tierSnapshot(userScore.getCurrentTier().getTierCode())
                    .eventId(request.eventId())
                    .build();
            pointExpirationBucketRepository.save(bucket);
        }

        if (tierChanged) {
            MembershipTierHistory tierHistory = MembershipTierHistory.builder()
                    .userScore(userScore)
                    .oldTierCode(previousTierCode)
                    .newTierCode(newTier.getTierCode())
                    .reason("Upgraded tier due to earning points from booking " + request.bookingId())
                    .build();
            membershipTierHistoryRepository.save(tierHistory);
            outboxService.saveEvent("TIER", String.valueOf(userScore.getUserId()), "TIER_UPGRADED", java.util.Map.of("userId", userScore.getUserId(), "oldTier", previousTierCode, "newTier", newTier.getTierCode()), org.slf4j.MDC.get("correlationId"));
        }

        ScoreEarnResponse response = new ScoreEarnResponse(
                earnedPoints,
                oldBalance,
                userScore.getCurrentPoints(),
                oldAccumulated,
                userScore.getAccumulatedPoints(),
                previousTierCode,
                userScore.getCurrentTier().getTierCode(),
                tierChanged,
                false
        );
        outboxService.saveEvent("USER_SCORE", String.valueOf(userScore.getUserId()), "POINT_EARNED", response, org.slf4j.MDC.get("correlationId"));
        metricsService.recordPointsEarned(earnedPoints);
        if (tierChanged) {
            metricsService.recordTierUpgrade();
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public RedeemPreviewResponse previewRedeem(Long userId, RedeemPreviewRequest request) {
        if (request.points() == null || request.points() <= 0) {
            throw new BusinessException("Points must be greater than zero", "SCORE_INVALID_POINT_AMOUNT", HttpStatus.BAD_REQUEST);
        }
        String bookingReference = request.bookingReference();
        if (bookingReference == null) {
            throw new BusinessException(
                    "Booking ID or public ID is required",
                    "SCORE_BOOKING_REFERENCE_REQUIRED",
                    HttpStatus.BAD_REQUEST);
        }
        if (request.bookingPublicId() != null && !request.bookingPublicId().isBlank()) {
            try {
                bookingReference = UUID.fromString(bookingReference).toString();
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(
                        "Booking public ID must be a valid UUID",
                        "SCORE_BOOKING_REFERENCE_INVALID",
                        HttpStatus.BAD_REQUEST);
            }
        } else if (request.bookingId() <= 0) {
            throw new BusinessException(
                    "Booking ID must be greater than zero",
                    "SCORE_BOOKING_REFERENCE_INVALID",
                    HttpStatus.BAD_REQUEST);
        }

        BookingContext booking = bookingInternalClient.getBookingContext(bookingReference);
        validateRedeemBooking(userId, booking);

        UserScore userScore = userScoreRepository.findByUserId(userId)
                .orElseGet(() -> getOrInitializeUserScore(userId));

        int availablePoints = userScore.getCurrentPoints() - userScore.getHeldPoints();
        BigDecimal redeemableBookingAmount = booking.getAmount()
                .subtract(BigDecimal.ONE)
                .max(BigDecimal.ZERO);
        int maxByBookingAmount = redeemableBookingAmount
                .divide(redemptionValuePerPoint, 0, RoundingMode.DOWN)
                .min(BigDecimal.valueOf(Integer.MAX_VALUE))
                .intValue();
        int maxRedeemablePoints = Math.max(0, Math.min(availablePoints, maxByBookingAmount));
        BigDecimal discountAmount = discountForPoints(request.points());
        boolean hasEnoughPoints = availablePoints >= request.points();
        boolean withinBookingAmount = request.points() <= maxByBookingAmount;
        boolean eligible = hasEnoughPoints && withinBookingAmount;
        String message = !hasEnoughPoints
                ? "Insufficient available points for redemption"
                : !withinBookingAmount
                    ? "Requested points exceed the payable Booking amount"
                    : "Eligible for redemption";

        return new RedeemPreviewResponse(
                eligible,
                request.points(),
                availablePoints,
                maxRedeemablePoints,
                discountAmount,
                booking.getAmount(),
                booking.getAmount().subtract(discountAmount).max(BigDecimal.ZERO),
                redemptionValuePerPoint,
                message);
    }

    private void validateRedeemBooking(Long userId, BookingContext booking) {
        if (booking == null) {
            throw new BusinessException(
                    "Booking Service returned an empty payment context",
                    "SCORE_BOOKING_CONTEXT_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY);
        }
        if (!userId.equals(booking.getUserId())) {
            throw new BusinessException(
                    "Booking does not belong to the authenticated customer",
                    "SCORE_BOOKING_OWNER_MISMATCH",
                    HttpStatus.FORBIDDEN);
        }
        if (!booking.isRedeemAllowed()
                || !"PENDING_PAYMENT".equalsIgnoreCase(booking.getStatus())
                || booking.getAmount() == null
                || booking.getAmount().signum() <= 0) {
            throw new BusinessException(
                    "Booking is not eligible for point redemption",
                    "SCORE_BOOKING_NOT_REDEEMABLE",
                    HttpStatus.CONFLICT);
        }
        if (booking.getExpiresAt() == null || !booking.getExpiresAt().isAfter(java.time.Instant.now())) {
            throw new BusinessException(
                    "Booking payment deadline has expired",
                    "SCORE_BOOKING_EXPIRED",
                    HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public ScoreHoldResponse holdPoints(ScoreHoldRequest request) {
        if (request.points() == null || request.points() <= 0) {
            throw new BusinessException("Points must be greater than zero", "SCORE_INVALID_POINT_AMOUNT", HttpStatus.BAD_REQUEST);
        }
        BigDecimal discountAmount = discountForPoints(request.points());
        if (request.bookingAmount() != null
                && (request.bookingAmount().signum() <= 0
                || discountAmount.compareTo(request.bookingAmount()) >= 0)) {
            throw new BusinessException(
                    "Point discount must be lower than the payable Booking amount",
                    "SCORE_DISCOUNT_EXCEEDS_BOOKING_AMOUNT",
                    HttpStatus.BAD_REQUEST);
        }

        Optional<ScoreHold> existingOpt = scoreHoldRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingOpt.isPresent()) {
            ScoreHold hold = existingOpt.get();
            UserScore us = hold.getUserScore();
            if (!us.getUserId().equals(request.userId())
                    || !hold.getBookingId().equals(request.bookingId())
                    || !hold.getPoints().equals(request.points())) {
                throw new BusinessException(
                        "Idempotency key was already used for another score hold",
                        "SCORE_IDEMPOTENCY_CONFLICT",
                        HttpStatus.CONFLICT);
            }
            return new ScoreHoldResponse(
                    hold.getHoldCode(),
                    us.getUserId(),
                    hold.getBookingId(),
                    hold.getPoints(),
                    us.getCurrentPoints() - us.getHeldPoints(),
                    hold.getExpiredAt(),
                    hold.getStatus().name(),
                    true,
                    discountForPoints(hold.getPoints()),
                    redemptionValuePerPoint
            );
        }

        Optional<ScoreHold> byBooking = scoreHoldRepository.findByBookingId(request.bookingId());
        if (byBooking.isPresent() && byBooking.get().getStatus() == ScoreHoldStatus.ACTIVE) {
            throw new BusinessException("An active hold already exists for this booking", "SCORE_HOLD_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        UserScore userScore = getOrInitializeUserScore(request.userId());

        if (userScore.getStatus() == UserScoreStatus.LOCKED) {
            throw new BusinessException("User membership account is locked", "SCORE_ACCOUNT_LOCKED", HttpStatus.FORBIDDEN);
        }

        int availablePoints = userScore.getCurrentPoints() - userScore.getHeldPoints();
        if (request.points() > availablePoints) {
            throw new BusinessException("Insufficient available points to hold", "SCORE_INSUFFICIENT_POINTS", HttpStatus.BAD_REQUEST);
        }

        int ttl = (request.ttlSeconds() != null && request.ttlSeconds() > 0) ? request.ttlSeconds() : 900;
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(ttl);
        String holdCode = "HOLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ScoreHold hold = ScoreHold.builder()
                .holdCode(holdCode)
                .userScore(userScore)
                .bookingId(request.bookingId())
                .points(request.points())
                .status(ScoreHoldStatus.ACTIVE)
                .expiredAt(expiredAt)
                .eventId(request.eventId())
                .idempotencyKey(request.idempotencyKey())
                .sourceService("BOOKING")
                .build();
        scoreHoldRepository.save(hold);

        int heldBefore = userScore.getHeldPoints();
        userScore.setHeldPoints(heldBefore + request.points());
        userScoreRepository.save(userScore);

        ScoreHistory history = ScoreHistory.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userScore(userScore)
                .bookingId(request.bookingId())
                .holdId(hold.getId())
                .eventId(request.eventId())
                .idempotencyKey(request.idempotencyKey())
                .transactionType(ScoreTransactionType.HOLD)
                .pointChange(0)
                .actualPointChange(0)
                .balanceBefore(userScore.getCurrentPoints())
                .balanceAfter(userScore.getCurrentPoints())
                .heldBefore(heldBefore)
                .heldAfter(userScore.getHeldPoints())
                .accumulatedBefore(userScore.getAccumulatedPoints())
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .outstandingBefore(userScore.getOutstandingPoints())
                .outstandingAfter(userScore.getOutstandingPoints())
                .tierSnapshot(userScore.getCurrentTier().getTierCode())
                .earningRateSnapshot(userScore.getCurrentTier().getEarningRate())
                .sourceService("BOOKING")
                .description("Held " + request.points() + " points for booking " + request.bookingId())
                .build();
        scoreHistoryRepository.save(history);

        ScoreHoldResponse response = new ScoreHoldResponse(
                hold.getHoldCode(),
                userScore.getUserId(),
                hold.getBookingId(),
                hold.getPoints(),
                userScore.getCurrentPoints() - userScore.getHeldPoints(),
                hold.getExpiredAt(),
                hold.getStatus().name(),
                false,
                discountAmount,
                redemptionValuePerPoint
        );
        outboxService.saveEvent("HOLD", hold.getHoldCode(), "POINT_HELD", response, org.slf4j.MDC.get("correlationId"));
        return response;
    }

    private BigDecimal discountForPoints(int points) {
        return redemptionValuePerPoint
                .multiply(BigDecimal.valueOf(points))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public ScoreCommitResponse commitPoints(ScoreCommitRequest request) {
        Optional<ScoreHistory> existingHist = scoreHistoryRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingHist.isPresent()) {
            ScoreHistory sh = existingHist.get();
            UserScore us = sh.getUserScore();
            String code = (sh.getHoldId() != null) ? scoreHoldRepository.findById(sh.getHoldId()).map(ScoreHold::getHoldCode).orElse("") : "";
            int pointsCommitted = sh.getActualPointChange() != null ? Math.abs(sh.getActualPointChange()) : Math.abs(sh.getPointChange());
            return new ScoreCommitResponse(
                    code,
                    us.getUserId(),
                    sh.getBookingId(),
                    pointsCommitted,
                    sh.getBalanceBefore(),
                    sh.getBalanceAfter(),
                    us.getHeldPoints(),
                    us.getHeldPoints(),
                    ScoreHoldStatus.COMMITTED.name(),
                    true
            );
        }

        ScoreHold hold;
        if (request.holdCode() != null && !request.holdCode().isEmpty()) {
            hold = scoreHoldRepository.findWithLockByHoldCode(request.holdCode())
                    .orElseThrow(() -> new BusinessException("Hold not found", "SCORE_HOLD_NOT_FOUND", HttpStatus.NOT_FOUND));
        } else {
            hold = scoreHoldRepository.findWithLockByBookingId(request.bookingId())
                    .orElseThrow(() -> new BusinessException("Hold not found for booking ID", "SCORE_HOLD_NOT_FOUND", HttpStatus.NOT_FOUND));
        }

        if (hold.getStatus() == ScoreHoldStatus.COMMITTED) {
            throw new BusinessException("Hold has already been committed", "SCORE_HOLD_ALREADY_COMMITTED", HttpStatus.CONFLICT);
        }
        if (hold.getStatus() == ScoreHoldStatus.RELEASED) {
            throw new BusinessException("Hold has already been released", "SCORE_HOLD_ALREADY_RELEASED", HttpStatus.CONFLICT);
        }
        if (hold.getStatus() == ScoreHoldStatus.EXPIRED || hold.getExpiredAt().isBefore(LocalDateTime.now())) {
            if (hold.getStatus() != ScoreHoldStatus.EXPIRED) {
                hold.setStatus(ScoreHoldStatus.EXPIRED);
                scoreHoldRepository.save(hold);
            }
            throw new BusinessException("Hold has expired", "SCORE_HOLD_EXPIRED", HttpStatus.BAD_REQUEST);
        }

        UserScore userScore = userScoreRepository.findWithLockByUserId(hold.getUserScore().getUserId())
                .orElseThrow(() -> new BusinessException("User score not found", "SCORE_USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        int pointsToCommit = hold.getPoints();
        if (userScore.getCurrentPoints() < pointsToCommit || userScore.getHeldPoints() < pointsToCommit) {
            throw new BusinessException("Insufficient points to commit", "SCORE_INSUFFICIENT_POINTS", HttpStatus.BAD_REQUEST);
        }

        int balanceBefore = userScore.getCurrentPoints();
        int heldBefore = userScore.getHeldPoints();

        userScore.setCurrentPoints(balanceBefore - pointsToCommit);
        userScore.setHeldPoints(heldBefore - pointsToCommit);
        userScore.setLastRedeemAt(LocalDateTime.now());
        userScoreRepository.save(userScore);

        hold.setStatus(ScoreHoldStatus.COMMITTED);
        hold.setCommittedAt(LocalDateTime.now());
        scoreHoldRepository.save(hold);

        consumePointsFromBuckets(userScore.getUserId(), pointsToCommit);

        ScoreHistory history = ScoreHistory.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userScore(userScore)
                .bookingId(hold.getBookingId())
                .holdId(hold.getId())
                .eventId(request.eventId())
                .idempotencyKey(request.idempotencyKey())
                .transactionType(ScoreTransactionType.REDEEM)
                .pointChange(-pointsToCommit)
                .actualPointChange(-pointsToCommit)
                .balanceBefore(balanceBefore)
                .balanceAfter(userScore.getCurrentPoints())
                .heldBefore(heldBefore)
                .heldAfter(userScore.getHeldPoints())
                .accumulatedBefore(userScore.getAccumulatedPoints())
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .outstandingBefore(userScore.getOutstandingPoints())
                .outstandingAfter(userScore.getOutstandingPoints())
                .tierSnapshot(userScore.getCurrentTier().getTierCode())
                .earningRateSnapshot(userScore.getCurrentTier().getEarningRate())
                .redeemRateSnapshot(redemptionValuePerPoint)
                .sourceService("BOOKING")
                .description("Committed points for booking " + hold.getBookingId())
                .build();
        scoreHistoryRepository.save(history);

        ScoreCommitResponse response = new ScoreCommitResponse(
                hold.getHoldCode(),
                userScore.getUserId(),
                hold.getBookingId(),
                pointsToCommit,
                balanceBefore,
                userScore.getCurrentPoints(),
                heldBefore,
                userScore.getHeldPoints(),
                ScoreHoldStatus.COMMITTED.name(),
                false
        );
        outboxService.saveEvent("HOLD", hold.getHoldCode(), "POINT_COMMITTED", response, org.slf4j.MDC.get("correlationId"));
        metricsService.recordPointsRedeemed(pointsToCommit);
        return response;
    }

    @Override
    @Transactional
    public ScoreReleaseResponse releasePoints(ScoreReleaseRequest request) {
        Optional<ScoreHistory> existingHist = scoreHistoryRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingHist.isPresent()) {
            ScoreHistory sh = existingHist.get();
            UserScore us = sh.getUserScore();
            String code = (sh.getHoldId() != null) ? scoreHoldRepository.findById(sh.getHoldId()).map(ScoreHold::getHoldCode).orElse("") : "";
            return new ScoreReleaseResponse(
                    code,
                    us.getUserId(),
                    sh.getBookingId(),
                    0,
                    us.getCurrentPoints() - us.getHeldPoints(),
                    us.getHeldPoints(),
                    us.getHeldPoints(),
                    ScoreHoldStatus.RELEASED.name(),
                    true
            );
        }

        ScoreHold hold;
        if (request.holdCode() != null && !request.holdCode().isEmpty()) {
            hold = scoreHoldRepository.findWithLockByHoldCode(request.holdCode())
                    .orElseThrow(() -> new BusinessException("Hold not found", "SCORE_HOLD_NOT_FOUND", HttpStatus.NOT_FOUND));
        } else {
            hold = scoreHoldRepository.findWithLockByBookingId(request.bookingId())
                    .orElseThrow(() -> new BusinessException("Hold not found for booking ID", "SCORE_HOLD_NOT_FOUND", HttpStatus.NOT_FOUND));
        }

        if (hold.getStatus() == ScoreHoldStatus.RELEASED) {
            throw new BusinessException("Hold has already been released", "SCORE_HOLD_ALREADY_RELEASED", HttpStatus.CONFLICT);
        }
        if (hold.getStatus() == ScoreHoldStatus.COMMITTED) {
            throw new BusinessException("Hold has already been committed and cannot be released", "SCORE_HOLD_ALREADY_COMMITTED", HttpStatus.CONFLICT);
        }

        UserScore userScore = userScoreRepository.findWithLockByUserId(hold.getUserScore().getUserId())
                .orElseThrow(() -> new BusinessException("User score not found", "SCORE_USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        int heldBefore = userScore.getHeldPoints();
        int pointsToRelease = hold.getPoints();
        userScore.setHeldPoints(Math.max(0, heldBefore - pointsToRelease));
        userScoreRepository.save(userScore);

        hold.setStatus(ScoreHoldStatus.RELEASED);
        hold.setReleasedAt(LocalDateTime.now());
        scoreHoldRepository.save(hold);

        ScoreHistory history = ScoreHistory.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userScore(userScore)
                .bookingId(hold.getBookingId())
                .holdId(hold.getId())
                .eventId(request.eventId())
                .idempotencyKey(request.idempotencyKey())
                .transactionType(ScoreTransactionType.RELEASE)
                .pointChange(0)
                .actualPointChange(0)
                .balanceBefore(userScore.getCurrentPoints())
                .balanceAfter(userScore.getCurrentPoints())
                .heldBefore(heldBefore)
                .heldAfter(userScore.getHeldPoints())
                .accumulatedBefore(userScore.getAccumulatedPoints())
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .outstandingBefore(userScore.getOutstandingPoints())
                .outstandingAfter(userScore.getOutstandingPoints())
                .tierSnapshot(userScore.getCurrentTier().getTierCode())
                .earningRateSnapshot(userScore.getCurrentTier().getEarningRate())
                .sourceService("BOOKING")
                .description("Released held points for booking " + hold.getBookingId() + (request.reason() != null ? ": " + request.reason() : ""))
                .build();
        scoreHistoryRepository.save(history);

        ScoreReleaseResponse response = new ScoreReleaseResponse(
                hold.getHoldCode(),
                userScore.getUserId(),
                hold.getBookingId(),
                pointsToRelease,
                userScore.getCurrentPoints() - userScore.getHeldPoints(),
                heldBefore,
                userScore.getHeldPoints(),
                ScoreHoldStatus.RELEASED.name(),
                false
        );
        outboxService.saveEvent("HOLD", hold.getHoldCode(), "POINT_RELEASED", response, org.slf4j.MDC.get("correlationId"));
        return response;
    }

    private void consumePointsFromBuckets(Long userId, int pointsToConsume) {
        if (pointsToConsume <= 0) return;
        List<PointExpirationBucket> activeBuckets = pointExpirationBucketRepository
                .findWithLockByUserIdAndStatusIn(userId, List.of(PointExpirationBucketStatus.ACTIVE, PointExpirationBucketStatus.PARTIAL));
        int remaining = pointsToConsume;
        for (PointExpirationBucket bucket : activeBuckets) {
            if (remaining <= 0) break;
            int canConsume = Math.min(bucket.getRemainingPoints(), remaining);
            bucket.setRemainingPoints(bucket.getRemainingPoints() - canConsume);
            bucket.setConsumedPoints(bucket.getConsumedPoints() + canConsume);
            if (bucket.getRemainingPoints() == 0) {
                bucket.setStatus(PointExpirationBucketStatus.CONSUMED);
            } else {
                bucket.setStatus(PointExpirationBucketStatus.PARTIAL);
            }
            pointExpirationBucketRepository.save(bucket);
            remaining -= canConsume;
        }
    }

    @Override
    @Transactional
    public ScoreRedeemResponse redeemPoints(ScoreRedeemRequest request) {
        if (request.userId() == null || request.bookingId() == null || request.points() == null || request.points() <= 0
                || request.eventId() == null || request.eventId().trim().isEmpty()) {
            throw new BusinessException("Invalid redeem request", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        Optional<ScoreHistory> existingOpt = scoreHistoryRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingOpt.isEmpty() && request.eventId() != null && !request.eventId().isEmpty()) {
            existingOpt = scoreHistoryRepository.findByEventId(request.eventId());
        }
        if (existingOpt.isPresent()) {
            ScoreHistory sh = existingOpt.get();
            int pts = sh.getActualPointChange() != null ? Math.abs(sh.getActualPointChange()) : Math.abs(sh.getPointChange());
            if (!sh.getUserScore().getUserId().equals(request.userId()) ||
                !sh.getBookingId().equals(request.bookingId()) ||
                !sh.getEventId().equals(request.eventId()) ||
                pts != request.points()) {
                throw new BusinessException("Idempotency conflict", "SCORE_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT);
            }

            return new ScoreRedeemResponse(
                    sh.getUserScore().getUserId(),
                    sh.getBookingId(),
                    pts,
                    discountForPoints(pts).intValueExact(),
                    sh.getBalanceAfter(),
                    sh.getAccumulatedAfter(),
                    true
            );
        }

        UserScore userScore = getOrInitializeUserScore(request.userId());

        if (userScore.getStatus() == UserScoreStatus.LOCKED) {
            throw new BusinessException("User membership account is locked", "SCORE_ACCOUNT_LOCKED", HttpStatus.FORBIDDEN);
        }

        int availablePoints = userScore.getCurrentPoints() - userScore.getHeldPoints();
        if (availablePoints < request.points()) {
            java.util.Map<String, Object> errData = java.util.Map.of(
                    "availablePoints", availablePoints,
                    "requestedPoints", request.points()
            );
            throw new BusinessException("Insufficient available points for redemption", "SCORE_INSUFFICIENT_BALANCE", HttpStatus.CONFLICT, errData);
        }

        int oldBalance = userScore.getCurrentPoints();
        userScore.setCurrentPoints(oldBalance - request.points());
        userScore.setLastRedeemAt(LocalDateTime.now());
        userScoreRepository.save(userScore);

        consumePointsFromBuckets(userScore.getUserId(), request.points());

        ScoreHistory history = ScoreHistory.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userScore(userScore)
                .bookingId(request.bookingId())
                .eventId(request.eventId())
                .idempotencyKey(request.idempotencyKey())
                .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                .pointChange(-request.points())
                .actualPointChange(-request.points())
                .balanceBefore(oldBalance)
                .balanceAfter(userScore.getCurrentPoints())
                .heldBefore(userScore.getHeldPoints())
                .heldAfter(userScore.getHeldPoints())
                .accumulatedBefore(userScore.getAccumulatedPoints())
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .outstandingBefore(userScore.getOutstandingPoints())
                .outstandingAfter(userScore.getOutstandingPoints())
                .tierSnapshot(userScore.getCurrentTier().getTierCode())
                .earningRateSnapshot(userScore.getCurrentTier().getEarningRate())
                .redeemRateSnapshot(redemptionValuePerPoint)
                .sourceService("BOOKING")
                .description("Redeemed points for booking " + request.bookingId())
                .build();
        scoreHistoryRepository.save(history);

        ScoreRedeemResponse response = new ScoreRedeemResponse(
                userScore.getUserId(),
                request.bookingId(),
                request.points(),
                redemptionValuePerPoint.multiply(BigDecimal.valueOf(request.points())).intValueExact(),
                userScore.getCurrentPoints(),
                userScore.getAccumulatedPoints(),
                false
        );
        outboxService.saveEvent("USER_SCORE", String.valueOf(userScore.getUserId()), "POINT_REDEEMED", response, org.slf4j.MDC.get("correlationId"));
        metricsService.recordPointsRedeemed(request.points());
        return response;
    }

    @Override
    @Transactional
    public ScoreRefundResponse refundRedeem(ScoreRefundRequest request) {
        if (request.userId() == null || request.pointsToRefund() == null || request.pointsToRefund() <= 0
                || request.eventId() == null || request.eventId().trim().isEmpty()
                || request.idempotencyKey() == null || request.idempotencyKey().trim().isEmpty()) {
            throw new BusinessException("Invalid refund request", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        Optional<ScoreHistory> existingOpt = scoreHistoryRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingOpt.isEmpty() && request.eventId() != null && !request.eventId().isEmpty()) {
            existingOpt = scoreHistoryRepository.findByEventId(request.eventId());
        }
        if (existingOpt.isPresent()) {
            ScoreHistory sh = existingOpt.get();
            return new ScoreRefundResponse(
                    sh.getUserScore().getUserId(),
                    sh.getBookingId(),
                    Math.abs(sh.getPointChange()),
                    sh.getBalanceAfter(),
                    sh.getAccumulatedAfter(),
                    sh.getReferenceHistory() != null ? sh.getReferenceHistory().getId() : null,
                    true
            );
        }

        ScoreHistory orig = null;
        if (request.originalRedeemEventId() != null && !request.originalRedeemEventId().isEmpty()) {
            orig = scoreHistoryRepository.findByEventId(request.originalRedeemEventId()).orElse(null);
        }
        if (orig == null && request.bookingId() != null) {
            List<ScoreHistory> byBooking = scoreHistoryRepository.findByBookingId(request.bookingId());
            orig = byBooking.stream()
                    .filter(sh -> sh.getTransactionType() == ScoreTransactionType.REDEEM || sh.getTransactionType() == ScoreTransactionType.REDEEM_FOR_BOOKING)
                    .findFirst().orElse(null);
        }
        if (orig == null) {
            throw new BusinessException("Original transaction not found", "SCORE_ORIGINAL_TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        if (orig.getTransactionType() != ScoreTransactionType.REDEEM && orig.getTransactionType() != ScoreTransactionType.REDEEM_FOR_BOOKING) {
            throw new BusinessException("Invalid transaction type for refund", "SCORE_INVALID_TRANSACTION_TYPE", HttpStatus.BAD_REQUEST);
        }
        if (!orig.getUserScore().getUserId().equals(request.userId()) || (orig.getBookingId() != null && !orig.getBookingId().equals(request.bookingId()))) {
            throw new BusinessException("Transaction mismatch", "SCORE_TRANSACTION_MISMATCH", HttpStatus.BAD_REQUEST);
        }

        UserScore userScore = getOrInitializeUserScore(request.userId());

        List<ScoreHistory> refunds = scoreHistoryRepository.findByReferenceHistory(orig);
        int alreadyRefunded = 0;
        for (ScoreHistory r : refunds) {
            if (r.getTransactionType() == ScoreTransactionType.REFUND_REDEEM) {
                alreadyRefunded += Math.abs(r.getPointChange());
            }
        }
        int origRedeemed = Math.abs(orig.getPointChange());
        if (alreadyRefunded >= origRedeemed) {
            throw new BusinessException("Score already refunded", "SCORE_ALREADY_REFUNDED", HttpStatus.CONFLICT);
        }
        if (alreadyRefunded + request.pointsToRefund() > origRedeemed) {
            throw new BusinessException("Refund amount exceeds redeemed amount", "SCORE_INVALID_REFUND_AMOUNT", HttpStatus.BAD_REQUEST);
        }


        int oldBalance = userScore.getCurrentPoints();
        int oldAccumulated = userScore.getAccumulatedPoints();
        userScore.setCurrentPoints(oldBalance + request.pointsToRefund());
        userScoreRepository.save(userScore);

        PointExpirationBucket newBucket = PointExpirationBucket.builder()
                .userScore(userScore)
                .scoreHistory(orig)
                .bookingId(request.bookingId())
                .earnedPoints(request.pointsToRefund())
                .remainingPoints(request.pointsToRefund())
                .expiredPoints(0)
                .consumedPoints(0)
                .expirationDate(LocalDate.now().plusYears(1))
                .status(PointExpirationBucketStatus.ACTIVE)
                .tierSnapshot(userScore.getCurrentTier().getTierCode())
                .eventId(request.eventId())
                .build();
        pointExpirationBucketRepository.save(newBucket);

        ScoreHistory history = ScoreHistory.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userScore(userScore)
                .bookingId(request.bookingId())
                .eventId(request.eventId())
                .idempotencyKey(request.idempotencyKey())
                .transactionType(ScoreTransactionType.REFUND_REDEEM)
                .pointChange(request.pointsToRefund())
                .actualPointChange(request.pointsToRefund())
                .balanceBefore(oldBalance)
                .balanceAfter(userScore.getCurrentPoints())
                .heldBefore(userScore.getHeldPoints())
                .heldAfter(userScore.getHeldPoints())
                .accumulatedBefore(oldAccumulated)
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .outstandingBefore(userScore.getOutstandingPoints())
                .outstandingAfter(userScore.getOutstandingPoints())
                .tierSnapshot(userScore.getCurrentTier().getTierCode())
                .earningRateSnapshot(userScore.getCurrentTier().getEarningRate())
                .redeemRateSnapshot(redemptionValuePerPoint)
                .sourceService("PAYMENT")
                .referenceHistory(orig)
                .reason(request.reason())
                .description(request.reason() != null ? request.reason() : "Refunded redeemed points for booking " + request.bookingId())
                .build();

        scoreHistoryRepository.save(history);

        ScoreRefundResponse response = new ScoreRefundResponse(
                userScore.getUserId(),
                request.bookingId(),
                request.pointsToRefund(),
                userScore.getCurrentPoints(),
                userScore.getAccumulatedPoints(),
                orig.getId(),
                false
        );
        outboxService.saveEvent("USER_SCORE", String.valueOf(userScore.getUserId()), "POINT_REFUNDED", response, org.slf4j.MDC.get("correlationId"));
        return response;
    }

    @Override
    @Transactional
    public ScoreRevokeResponse revokeEarn(ScoreRevokeRequest request) {
        if (request.userId() == null || request.pointsToRevoke() == null || request.pointsToRevoke() <= 0
                || request.eventId() == null || request.eventId().trim().isEmpty()
                || request.idempotencyKey() == null || request.idempotencyKey().trim().isEmpty()) {
            throw new BusinessException("Invalid revoke request", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        Optional<ScoreHistory> existingOpt = scoreHistoryRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingOpt.isEmpty() && request.eventId() != null && !request.eventId().isEmpty()) {
            existingOpt = scoreHistoryRepository.findByEventId(request.eventId());
        }
        if (existingOpt.isPresent()) {
            ScoreHistory sh = existingOpt.get();
            UserScore us = sh.getUserScore();
            int reqPts = sh.getRequestedPointChange() != null ? Math.abs(sh.getRequestedPointChange()) : Math.abs(sh.getPointChange());
            if (!sh.getUserScore().getUserId().equals(request.userId()) ||
                (sh.getBookingId() != null && !sh.getBookingId().equals(request.bookingId())) ||
                !sh.getEventId().equals(request.eventId()) ||
                reqPts != request.pointsToRevoke()) {
                throw new BusinessException("Idempotency conflict", "SCORE_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT);
            }

            int dedPts = Math.abs(sh.getPointChange());
            int outPts = sh.getOutstandingAfter();
            return new ScoreRevokeResponse(
                    us.getUserId(),
                    reqPts,
                    dedPts,
                    outPts,
                    sh.getBalanceAfter(),
                    sh.getAccumulatedAfter(),
                    us.getCurrentTier().getTierCode(),
                    us.getCurrentTier().getTierCode(),
                    false,
                    sh.getReconciliationStatus() != null ? sh.getReconciliationStatus().name() : "NONE",
                    outPts > 0,
                    true
            );
        }

        ScoreHistory orig = null;
        if (request.originalEarnHistoryId() != null) {
            orig = scoreHistoryRepository.findById(request.originalEarnHistoryId()).orElse(null);
        }
        if (orig == null && request.originalEarnEventId() != null && !request.originalEarnEventId().isEmpty()) {
            orig = scoreHistoryRepository.findByEventId(request.originalEarnEventId()).orElse(null);
        }
        if (orig == null && request.bookingId() != null) {
            List<ScoreHistory> byBooking = scoreHistoryRepository.findByBookingId(request.bookingId());
            orig = byBooking.stream()
                    .filter(sh -> sh.getTransactionType() == ScoreTransactionType.EARN || sh.getTransactionType() == ScoreTransactionType.EARN_BY_BOOKING)
                    .findFirst().orElse(null);
        }
        if (orig == null) {
            throw new BusinessException("Original earn transaction invalid", "SCORE_ORIGINAL_TRANSACTION_INVALID", HttpStatus.CONFLICT);
        }
        if (orig.getTransactionType() != ScoreTransactionType.EARN && orig.getTransactionType() != ScoreTransactionType.EARN_BY_BOOKING) {
            throw new BusinessException("Wrong transaction type", "SCORE_ORIGINAL_TRANSACTION_INVALID", HttpStatus.CONFLICT);
        }
        if (!orig.getUserScore().getUserId().equals(request.userId()) || (orig.getBookingId() != null && !orig.getBookingId().equals(request.bookingId()))) {
            throw new BusinessException("User or booking mismatch", "SCORE_ORIGINAL_TRANSACTION_MISMATCH", HttpStatus.CONFLICT);
        }

        UserScore userScore = getOrInitializeUserScore(request.userId());

        List<ScoreHistory> revokes = scoreHistoryRepository.findByReferenceHistory(orig);
        int alreadyRevoked = 0;
        for (ScoreHistory r : revokes) {
            if (r.getTransactionType() == ScoreTransactionType.REVOKE_EARN || r.getTransactionType() == ScoreTransactionType.REVOKE_EARN_BY_REFUND) {
                int val = r.getRequestedPointChange() != null ? Math.abs(r.getRequestedPointChange()) : Math.abs(r.getPointChange());
                alreadyRevoked += val;
            }
        }
        int origPoints = orig.getActualPointChange() != null ? orig.getActualPointChange() : orig.getPointChange();
        if (alreadyRevoked >= origPoints) {
            throw new BusinessException("Revoke already processed", "SCORE_REVOKE_ALREADY_PROCESSED", HttpStatus.CONFLICT);
        }
        if (alreadyRevoked + request.pointsToRevoke() > origPoints) {
            throw new BusinessException("Revoke amount exceeds original earn", "SCORE_REVOKE_AMOUNT_EXCEEDS_ORIGINAL", HttpStatus.CONFLICT);
        }


        int toRevoke = request.pointsToRevoke();
        int deductedPoints = Math.min(userScore.getCurrentPoints(), toRevoke);
        int outstandingPoints = toRevoke - deductedPoints;

        int oldBalance = userScore.getCurrentPoints();
        int oldAccumulated = userScore.getAccumulatedPoints();
        int oldOutstanding = userScore.getOutstandingPoints();
        MembershipTier oldTier = userScore.getCurrentTier();
        String previousTierCode = oldTier.getTierCode();

        userScore.setCurrentPoints(oldBalance - deductedPoints);
        userScore.setAccumulatedPoints(Math.max(0, oldAccumulated - toRevoke));
        userScore.setOutstandingPoints(oldOutstanding + outstandingPoints);

        boolean tierChanged = false;
        List<MembershipTier> allTiers = membershipTierRepository.findAll();
        MembershipTier newTier = oldTier;
        MembershipTier bestTier = null;
        for (MembershipTier t : allTiers) {
            if (Boolean.TRUE.equals(t.getActive()) && userScore.getAccumulatedPoints() >= t.getMinAccumulatedPoints()) {
                if (bestTier == null || t.getMinAccumulatedPoints() >= bestTier.getMinAccumulatedPoints()) {
                    bestTier = t;
                }
            }
        }
        if (bestTier != null && !bestTier.getTierCode().equals(previousTierCode)) {
            userScore.setCurrentTier(bestTier);
            newTier = bestTier;
            tierChanged = true;
        }
        userScoreRepository.save(userScore);

        if (tierChanged) {
            MembershipTierHistory th = MembershipTierHistory.builder()
                    .userScore(userScore)
                    .oldTierCode(previousTierCode)
                    .newTierCode(newTier.getTierCode())
                    .reason("Downgraded tier due to earn revoke for booking " + request.bookingId())
                    .build();
            membershipTierHistoryRepository.save(th);
            outboxService.saveEvent("TIER", String.valueOf(userScore.getUserId()), "TIER_DOWNGRADED", java.util.Map.of("userId", userScore.getUserId(), "oldTier", previousTierCode, "newTier", newTier.getTierCode()), org.slf4j.MDC.get("correlationId"));
        }

        Optional<PointExpirationBucket> bucketOpt = pointExpirationBucketRepository.findWithLockByHistoryId(orig.getId());
        if (bucketOpt.isPresent()) {
            PointExpirationBucket bucket = bucketOpt.get();
            int consumeFromBucket = Math.min(bucket.getRemainingPoints(), toRevoke);
            bucket.setRemainingPoints(bucket.getRemainingPoints() - consumeFromBucket);
            bucket.setConsumedPoints(bucket.getConsumedPoints() + consumeFromBucket);
            if (bucket.getRemainingPoints() == 0) {
                bucket.setStatus(PointExpirationBucketStatus.CONSUMED);
            } else {
                bucket.setStatus(PointExpirationBucketStatus.PARTIAL);
            }
            pointExpirationBucketRepository.save(bucket);
        }

        ReconciliationStatus reconStatus = outstandingPoints > 0 ? ReconciliationStatus.PENDING : ReconciliationStatus.NONE;
        boolean requiresManual = outstandingPoints > 0;

        ScoreHistory history = ScoreHistory.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userScore(userScore)
                .bookingId(request.bookingId())
                .eventId(request.eventId())
                .idempotencyKey(request.idempotencyKey())
                .transactionType(ScoreTransactionType.REVOKE_EARN_BY_REFUND)
                .pointChange(-deductedPoints)
                .actualPointChange(-deductedPoints)
                .requestedPointChange(-toRevoke)
                .balanceBefore(oldBalance)
                .balanceAfter(userScore.getCurrentPoints())
                .heldBefore(userScore.getHeldPoints())
                .heldAfter(userScore.getHeldPoints())
                .accumulatedBefore(oldAccumulated)
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .outstandingBefore(oldOutstanding)
                .outstandingAfter(userScore.getOutstandingPoints())
                .tierSnapshot(previousTierCode)
                .earningRateSnapshot(orig.getEarningRateSnapshot())
                .sourceService("PAYMENT")
                .referenceHistory(orig)
                .reconciliationStatus(reconStatus)
                .reason(request.reason())
                .description(request.reason() != null ? request.reason() : "Revoked points for booking " + request.bookingId())
                .build();

        scoreHistoryRepository.save(history);

        ScoreRevokeResponse response = new ScoreRevokeResponse(
                userScore.getUserId(),
                toRevoke,
                deductedPoints,
                outstandingPoints,
                userScore.getCurrentPoints(),
                userScore.getAccumulatedPoints(),
                previousTierCode,
                newTier.getTierCode(),
                tierChanged,
                reconStatus.name(),
                requiresManual,
                false
        );
        outboxService.saveEvent("USER_SCORE", String.valueOf(userScore.getUserId()), "POINT_REVOKED", response, org.slf4j.MDC.get("correlationId"));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiringPointResponse> getExpiringPoints(Long userId) {
        List<PointExpirationBucket> buckets = pointExpirationBucketRepository.findByUserScore_UserIdOrderByExpirationDateAsc(userId);
        return buckets.stream()
                .map(b -> new ExpiringPointResponse(
                        b.getId(),
                        b.getEarnedPoints(),
                        b.getRemainingPoints(),
                        b.getExpirationDate(),
                        b.getStatus().name(),
                        b.getBookingId(),
                        b.getCreatedAt()
                )).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TierHistoryItemResponse> getTierHistory(Long userId) {
        List<MembershipTierHistory> histories = membershipTierHistoryRepository.findByUserScore_UserIdOrderByCreatedAtDesc(userId);
        return histories.stream()
                .map(h -> new TierHistoryItemResponse(
                        h.getId(),
                        h.getOldTierCode(),
                        h.getNewTierCode(),
                        h.getReason(),
                        h.getCreatedAt()
                )).toList();
    }

    @Override
    public void expirePoints() {
        int batchSize = 500;
        Pageable pageable = PageRequest.of(0, batchSize);
        List<PointExpirationBucketStatus> statuses = List.of(PointExpirationBucketStatus.ACTIVE, PointExpirationBucketStatus.PARTIAL);

        while (true) {
            Page<PointExpirationBucket> page = pointExpirationBucketRepository.findByStatusInAndExpirationDateBefore(statuses, LocalDate.now(), pageable);
            List<PointExpirationBucket> expiredBuckets = page.getContent();
            if (expiredBuckets.isEmpty()) {
                break;
            }
            for (PointExpirationBucket b : expiredBuckets) {
                try {
                    self.expireSingleBucket(b.getId());
                } catch (Exception e) {
                    log.error("Failed to expire point bucket id {}: ", b.getId(), e);
                }
            }
            if (expiredBuckets.size() < batchSize) {
                break;
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireSingleBucket(Long bucketId) {
        PointExpirationBucket b = pointExpirationBucketRepository.findById(bucketId).orElse(null);
        if (b == null || (b.getStatus() != PointExpirationBucketStatus.ACTIVE && b.getStatus() != PointExpirationBucketStatus.PARTIAL)) {
            return;
        }
        if (b.getRemainingPoints() > 0) {
            UserScore us = userScoreRepository.findWithLockByUserId(b.getUserScore().getUserId()).orElse(null);
            if (us != null && us.getCurrentPoints() > 0) {
                int toExpire = Math.min(us.getCurrentPoints(), b.getRemainingPoints());
                int oldBal = us.getCurrentPoints();
                us.setCurrentPoints(oldBal - toExpire);
                us.setLastExpireAt(LocalDateTime.now());
                userScoreRepository.save(us);

                b.setExpiredPoints(b.getExpiredPoints() + toExpire);
                b.setRemainingPoints(0);
                b.setStatus(PointExpirationBucketStatus.EXPIRED);
                pointExpirationBucketRepository.save(b);

                ScoreHistory history = ScoreHistory.builder()
                        .transactionUuid(UUID.randomUUID().toString())
                        .userScore(us)
                        .bookingId(b.getBookingId())
                        .eventId("expire-" + b.getId() + "-" + LocalDate.now())
                        .idempotencyKey("idem-expire-" + b.getId() + "-" + LocalDate.now())
                        .transactionType(ScoreTransactionType.EXPIRED)
                        .pointChange(-toExpire)
                        .actualPointChange(-toExpire)
                        .balanceBefore(oldBal)
                        .balanceAfter(us.getCurrentPoints())
                        .heldBefore(us.getHeldPoints())
                        .heldAfter(us.getHeldPoints())
                        .accumulatedBefore(us.getAccumulatedPoints())
                        .accumulatedAfter(us.getAccumulatedPoints())
                        .outstandingBefore(us.getOutstandingPoints())
                        .outstandingAfter(us.getOutstandingPoints())
                        .tierSnapshot(us.getCurrentTier().getTierCode())
                        .earningRateSnapshot(us.getCurrentTier().getEarningRate())
                        .sourceService("EXPIRATION_SCHEDULER")
                        .description("Expired points from bucket " + b.getId())
                        .build();
                scoreHistoryRepository.save(history);
                outboxService.saveEvent("USER_SCORE", String.valueOf(us.getUserId()), "POINT_EXPIRED", java.util.Map.of("userId", us.getUserId(), "expiredPoints", toExpire, "bucketId", b.getId()), "scheduler-expire");
                metricsService.recordPointsExpired(toExpire);
            } else {
                b.setStatus(PointExpirationBucketStatus.EXPIRED);
                pointExpirationBucketRepository.save(b);
            }
        } else {
            b.setStatus(PointExpirationBucketStatus.EXPIRED);
            pointExpirationBucketRepository.save(b);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserScore initializeUserScoreRequiresNew(Long userId) {
        MembershipTier defaultTier = membershipTierRepository.findFirstByIsActiveTrueOrderByMinAccumulatedPointsAsc()
                .orElseThrow(() -> new BusinessException("No active membership tier found in system", "SCORE_TIER_NOT_FOUND", HttpStatus.INTERNAL_SERVER_ERROR));

        UserScore newScore = new UserScore(
                userId,
                0,
                0,
                0,
                defaultTier,
                UserScoreStatus.ACTIVE,
                0,
                null,
                null,
                null,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        return userScoreRepository.save(newScore);
    }
}

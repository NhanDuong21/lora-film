package com.project.scoreservice.service.impl;

import com.project.scoreservice.dto.*;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.ScoreHistory;
import com.project.scoreservice.entity.ScoreHold;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.enumtype.ScoreHoldStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.enumtype.UserScoreStatus;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.repository.ScoreHistoryRepository;
import com.project.scoreservice.repository.ScoreHoldRepository;
import com.project.scoreservice.repository.UserScoreRepository;
import com.project.scoreservice.service.ScoreService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ScoreServiceImpl implements ScoreService {

    private final UserScoreRepository userScoreRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final ScoreHoldRepository scoreHoldRepository;

    public ScoreServiceImpl(UserScoreRepository userScoreRepository,
                            MembershipTierRepository membershipTierRepository,
                            ScoreHistoryRepository scoreHistoryRepository,
                            ScoreHoldRepository scoreHoldRepository) {
        this.userScoreRepository = userScoreRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.scoreHistoryRepository = scoreHistoryRepository;
        this.scoreHoldRepository = scoreHoldRepository;
    }

    @Override
    @Transactional
    public UserScoreResponse getUserScore(Long userId) {
        UserScore userScore = getOrInitializeUserScore(userId);
        return mapToUserScoreResponse(userScore);
    }

    @Override
    @Transactional
    public UserScoreResponse getUserTier(Long userId) {
        return getUserScore(userId);
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
        return userScoreRepository.findByUserId(userId).orElseGet(() -> {
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
        });
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

        UserScore userScore = userScoreRepository.findWithLockByUserId(request.userId())
                .orElseGet(() -> getOrInitializeUserScore(request.userId()));

        if (userScore.getStatus() == UserScoreStatus.LOCKED) {
            throw new BusinessException("User membership account is locked", "SCORE_ACCOUNT_LOCKED", HttpStatus.FORBIDDEN);
        }

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
                .accumulatedBefore(oldAccumulated)
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .description("Earned points for booking " + request.bookingId())
                .build();
        scoreHistoryRepository.save(history);

        return new ScoreEarnResponse(
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
    }

    @Override
    @Transactional(readOnly = true)
    public RedeemPreviewResponse previewRedeem(Long userId, RedeemPreviewRequest request) {
        if (request.points() == null || request.points() <= 0) {
            throw new BusinessException("Points must be greater than zero", "SCORE_INVALID_POINT_AMOUNT", HttpStatus.BAD_REQUEST);
        }

        UserScore userScore = userScoreRepository.findByUserId(userId)
                .orElseGet(() -> getOrInitializeUserScore(userId));

        int availablePoints = userScore.getCurrentPoints() - userScore.getHeldPoints();
        boolean eligible = availablePoints >= request.points();
        BigDecimal valuePerPoint = new BigDecimal("1000");
        BigDecimal discountAmount = new BigDecimal(request.points()).multiply(valuePerPoint);
        String message = eligible ? "Eligible for redemption" : "Insufficient available points for redemption";

        return new RedeemPreviewResponse(eligible, request.points(), availablePoints, discountAmount, valuePerPoint, message);
    }

    @Override
    @Transactional
    public ScoreHoldResponse holdPoints(ScoreHoldRequest request) {
        if (request.points() == null || request.points() <= 0) {
            throw new BusinessException("Points must be greater than zero", "SCORE_INVALID_POINT_AMOUNT", HttpStatus.BAD_REQUEST);
        }

        Optional<ScoreHold> existingOpt = scoreHoldRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingOpt.isPresent()) {
            ScoreHold hold = existingOpt.get();
            UserScore us = hold.getUserScore();
            return new ScoreHoldResponse(
                    hold.getHoldCode(),
                    us.getUserId(),
                    hold.getBookingId(),
                    hold.getPoints(),
                    us.getCurrentPoints() - us.getHeldPoints(),
                    hold.getExpiredAt(),
                    hold.getStatus().name(),
                    true
            );
        }

        Optional<ScoreHold> byBooking = scoreHoldRepository.findByBookingId(request.bookingId());
        if (byBooking.isPresent() && byBooking.get().getStatus() == ScoreHoldStatus.ACTIVE) {
            throw new BusinessException("An active hold already exists for this booking", "SCORE_HOLD_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        UserScore userScore = userScoreRepository.findWithLockByUserId(request.userId())
                .orElseGet(() -> getOrInitializeUserScore(request.userId()));

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

        userScore.setHeldPoints(userScore.getHeldPoints() + request.points());
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
                .accumulatedBefore(userScore.getAccumulatedPoints())
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .description("Held " + request.points() + " points for booking " + request.bookingId())
                .build();
        scoreHistoryRepository.save(history);

        return new ScoreHoldResponse(
                hold.getHoldCode(),
                userScore.getUserId(),
                hold.getBookingId(),
                hold.getPoints(),
                userScore.getCurrentPoints() - userScore.getHeldPoints(),
                hold.getExpiredAt(),
                hold.getStatus().name(),
                false
        );
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

        if (userScore.getStatus() == UserScoreStatus.LOCKED) {
            throw new BusinessException("User membership account is locked", "SCORE_ACCOUNT_LOCKED", HttpStatus.FORBIDDEN);
        }

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
                .accumulatedBefore(userScore.getAccumulatedPoints())
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .description("Committed points for booking " + hold.getBookingId())
                .build();
        scoreHistoryRepository.save(history);

        return new ScoreCommitResponse(
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
                .accumulatedBefore(userScore.getAccumulatedPoints())
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .description("Released held points for booking " + hold.getBookingId() + (request.reason() != null ? ": " + request.reason() : ""))
                .build();
        scoreHistoryRepository.save(history);

        return new ScoreReleaseResponse(
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
    }
}

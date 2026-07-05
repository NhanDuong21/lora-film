package com.project.scoreservice.service.impl;

import com.project.scoreservice.dto.ScoreAdjustmentRequest;
import com.project.scoreservice.dto.ScoreAdjustmentResponse;
import com.project.scoreservice.dto.ScoreAdjustmentType;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.ScoreHistory;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.repository.ScoreHistoryRepository;
import com.project.scoreservice.repository.UserScoreRepository;
import com.project.scoreservice.service.MembershipTierService;
import com.project.scoreservice.service.ScoreAdjustmentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ScoreAdjustmentServiceImpl implements ScoreAdjustmentService {

    private final UserScoreRepository userScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final MembershipTierService membershipTierService;
    private final MembershipTierRepository membershipTierRepository;

    public ScoreAdjustmentServiceImpl(UserScoreRepository userScoreRepository,
                                      ScoreHistoryRepository scoreHistoryRepository,
                                      MembershipTierService membershipTierService,
                                      MembershipTierRepository membershipTierRepository) {
        this.userScoreRepository = userScoreRepository;
        this.scoreHistoryRepository = scoreHistoryRepository;
        this.membershipTierService = membershipTierService;
        this.membershipTierRepository = membershipTierRepository;
    }

    @Override
    @Transactional
    public ScoreAdjustmentResponse adjustScore(Long userId, ScoreAdjustmentRequest request, Long adminId) {
        // 1. Check Idempotency by requestId
        Optional<ScoreHistory> existingHistoryOpt = scoreHistoryRepository.findByRequestId(request.getRequestId());
        if (existingHistoryOpt.isPresent()) {
            ScoreHistory existing = existingHistoryOpt.get();

            // Compare payloads
            boolean sameUser = existing.getUserScore().getUserId().equals(userId);

            ScoreAdjustmentType existingAdjType = existing.getTransactionType() == ScoreTransactionType.MANUAL_ADD
                    ? ScoreAdjustmentType.ADD : ScoreAdjustmentType.DEDUCT;
            boolean sameAdjType = existingAdjType == request.getAdjustmentType();

            int reqPointsAbs = Math.abs(existing.getRequestedPointChange());
            boolean samePoints = reqPointsAbs == request.getPoints();

            boolean affectedAccumulated = !existing.getAccumulatedBefore().equals(existing.getAccumulatedAfter());
            boolean sameAffectAccumulated = affectedAccumulated == request.getAffectAccumulatedPoints();

            if (!sameUser || !sameAdjType || !samePoints || !sameAffectAccumulated) {
                throw new BusinessException("Idempotency conflict: requestId was already used with a different payload",
                        "SCORE_ADJUSTMENT_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT);
            }

            // Payload matches -> return existing result
            UserScore userScore = existing.getUserScore();
            MembershipTier prevTier = membershipTierService.findTierForPoints(existing.getAccumulatedBefore());
            MembershipTier currTier = membershipTierService.findTierForPoints(existing.getAccumulatedAfter());

            return new ScoreAdjustmentResponse(
                    userId,
                    request.getAdjustmentType(),
                    existing.getPointChange(),
                    userScore.getCurrentPoints(),
                    userScore.getAccumulatedPoints(),
                    prevTier.getTierName(),
                    currTier.getTierName(),
                    !prevTier.getId().equals(currTier.getId()),
                    existing.getId(),
                    request.getRequestId(),
                    true // idempotent = true
            );
        }

        // 2. Resolve or lazy initialize score account (only for ADD)
        if (!userScoreRepository.existsById(userId)) {
            if (request.getAdjustmentType() == ScoreAdjustmentType.DEDUCT) {
                throw new BusinessException("User score account not found", "SCORE_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND);
            }

            // Lazy initialize account for ADD
            MembershipTier defaultTier = membershipTierRepository.findFirstByOrderByMinPointsAsc()
                    .orElseThrow(() -> new BusinessException("Membership tier configuration is invalid", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR));
            UserScore newUserScore = new UserScore(userId, 0, 0, defaultTier, null, null);
            try {
                userScoreRepository.saveAndFlush(newUserScore);
            } catch (Exception e) {
                // Ignore concurrent creation
            }
        }

        // 3. Lock user score
        UserScore userScore = userScoreRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));

        int balanceBefore = userScore.getCurrentPoints();
        int accumulatedBefore = userScore.getAccumulatedPoints();
        MembershipTier prevTier = membershipTierService.findTierForPoints(accumulatedBefore);

        int pointChange;
        int balanceAfter;
        int accumulatedAfter = accumulatedBefore;

        if (request.getAdjustmentType() == ScoreAdjustmentType.ADD) {
            pointChange = request.getPoints();
            long newCurrent = (long) balanceBefore + pointChange;
            long newAccumulated = (long) accumulatedBefore + (request.getAffectAccumulatedPoints() ? pointChange : 0);

            if (newCurrent > Integer.MAX_VALUE || newAccumulated > Integer.MAX_VALUE) {
                throw new BusinessException("Point value exceeds limit", "SCORE_POINT_OVERFLOW", HttpStatus.BAD_REQUEST);
            }

            balanceAfter = (int) newCurrent;
            if (request.getAffectAccumulatedPoints()) {
                accumulatedAfter = (int) newAccumulated;
            }
        } else {
            // DEDUCT
            pointChange = -request.getPoints();
            if (request.getPoints() > balanceBefore) {
                throw new BusinessException("Insufficient point balance for deduction", "SCORE_BALANCE_WOULD_BE_NEGATIVE", HttpStatus.CONFLICT);
            }
            if (request.getAffectAccumulatedPoints() && request.getPoints() > accumulatedBefore) {
                throw new BusinessException("Insufficient accumulated points for deduction", "SCORE_ACCUMULATED_POINTS_WOULD_BE_NEGATIVE", HttpStatus.CONFLICT);
            }

            balanceAfter = balanceBefore - request.getPoints();
            if (request.getAffectAccumulatedPoints()) {
                accumulatedAfter = accumulatedBefore - request.getPoints();
            }
        }

        // Apply changes
        userScore.setCurrentPoints(balanceAfter);
        userScore.setAccumulatedPoints(accumulatedAfter);

        MembershipTier currTier = prevTier;
        if (request.getAffectAccumulatedPoints()) {
            currTier = membershipTierService.findTierForPoints(accumulatedAfter);
            userScore.setCurrentTier(currTier);
        }

        userScoreRepository.saveAndFlush(userScore);

        // 4. Save history record
        ScoreTransactionType transType = request.getAdjustmentType() == ScoreAdjustmentType.ADD
                ? ScoreTransactionType.MANUAL_ADD : ScoreTransactionType.MANUAL_DEDUCT;

        String description = String.format("Manual adjustment (%s) of %d points by admin %d. Reason: %s",
                request.getAdjustmentType().name(), request.getPoints(), adminId, request.getReason());

        ScoreHistory history = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(null)
                .eventId(null)
                .pointChange(pointChange)
                .transactionType(transType)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .accumulatedBefore(accumulatedBefore)
                .accumulatedAfter(accumulatedAfter)
                .idempotencyKey("ADMIN_ADJUST:" + request.getRequestId())
                .referenceHistory(null)
                .createdBy(adminId)
                .requestId(request.getRequestId())
                .reason(request.getReason())
                .description(description)
                .requestedPointChange(pointChange)
                .outstandingPoints(0)
                .reconciliationStatus(ReconciliationStatus.NONE)
                .build();

        scoreHistoryRepository.saveAndFlush(history);

        boolean tierChanged = !prevTier.getId().equals(currTier.getId());

        return new ScoreAdjustmentResponse(
                userId,
                request.getAdjustmentType(),
                pointChange,
                balanceAfter,
                accumulatedAfter,
                prevTier.getTierName(),
                currTier.getTierName(),
                tierChanged,
                history.getId(),
                request.getRequestId(),
                false // idempotent = false (first time processed)
        );
    }
}

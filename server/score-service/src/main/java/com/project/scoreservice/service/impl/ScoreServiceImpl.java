package com.project.scoreservice.service.impl;

import com.project.scoreservice.dto.*;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.ScoreHistory;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.enumtype.UserScoreStatus;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.repository.ScoreHistoryRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ScoreServiceImpl implements ScoreService {

    private final UserScoreRepository userScoreRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;

    public ScoreServiceImpl(UserScoreRepository userScoreRepository,
                            MembershipTierRepository membershipTierRepository,
                            ScoreHistoryRepository scoreHistoryRepository) {
        this.userScoreRepository = userScoreRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.scoreHistoryRepository = scoreHistoryRepository;
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
}

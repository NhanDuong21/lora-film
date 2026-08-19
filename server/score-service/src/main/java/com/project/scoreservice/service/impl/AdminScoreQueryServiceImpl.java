package com.project.scoreservice.service.impl;

import com.project.scoreservice.dto.AdminScoreHistoryItemResponse;
import com.project.scoreservice.dto.AdminUserScoreResponse;
import com.project.scoreservice.dto.MembershipTierResponse;
import com.project.scoreservice.dto.NextTierResponse;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.ScoreHistory;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.repository.ScoreHistoryRepository;
import com.project.scoreservice.repository.UserScoreRepository;
import com.project.scoreservice.service.AdminScoreQueryService;
import com.project.scoreservice.service.MembershipTierService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AdminScoreQueryServiceImpl implements AdminScoreQueryService {

    private final UserScoreRepository userScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final MembershipTierService membershipTierService;
    private final MembershipTierRepository membershipTierRepository;

    public AdminScoreQueryServiceImpl(UserScoreRepository userScoreRepository,
                                      ScoreHistoryRepository scoreHistoryRepository,
                                      MembershipTierService membershipTierService,
                                      MembershipTierRepository membershipTierRepository) {
        this.userScoreRepository = userScoreRepository;
        this.scoreHistoryRepository = scoreHistoryRepository;
        this.membershipTierService = membershipTierService;
        this.membershipTierRepository = membershipTierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserScoreResponse getUserScoreDetail(Long userId) {
        UserScore userScore = userScoreRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));

        MembershipTier ct = membershipTierService.findTierForPoints(userScore.getAccumulatedPoints());
        MembershipTierResponse currentTier = new MembershipTierResponse(
                ct.getId(),
                ct.getTierCode(),
                ct.getTierName(),
                ct.getMinAccumulatedPoints(),
                ct.getEarningRate(),
                ct.getPriority()
        );

        Optional<MembershipTier> nextTierOpt = membershipTierRepository.findFirstByIsActiveTrueAndMinAccumulatedPointsGreaterThanOrderByMinAccumulatedPointsAsc(userScore.getAccumulatedPoints());
        NextTierResponse nextTier = null;
        if (nextTierOpt.isPresent()) {
            MembershipTier nt = nextTierOpt.get();
            int pointsReq = Math.max(0, nt.getMinAccumulatedPoints() - userScore.getAccumulatedPoints());
            nextTier = new NextTierResponse(nt.getId(), nt.getTierCode(), nt.getTierName(), nt.getMinAccumulatedPoints(), pointsReq);
        }

        return new AdminUserScoreResponse(
                userScore.getUserId(),
                userScore.getCurrentPoints(),
                userScore.getHeldPoints(),
                userScore.getAccumulatedPoints(),
                userScore.getOutstandingPoints(),
                userScore.getStatus(),
                currentTier,
                nextTier,
                userScore.getLastEarnAt(),
                userScore.getLastRedeemAt(),
                userScore.getLastExpireAt(),
                userScore.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminScoreHistoryItemResponse> getUserHistory(
            Long userId,
            int page,
            int size,
            ScoreTransactionType transactionType,
            Long bookingId,
            ReconciliationStatus reconciliationStatus,
            LocalDateTime from,
            LocalDateTime to,
            String sort
    ) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessException("Invalid page or size parameters", "SCORE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

        Sort.Direction direction = Sort.Direction.DESC;
        String sortField = "occurredAt";
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            sortField = sortParts[0];
            List<String> allowedSortFields = List.of("occurredAt", "createdAt", "actualPointChange", "balanceAfter", "bookingId", "id", "transactionType");
            if (!allowedSortFields.contains(sortField)) {
                throw new BusinessException("Invalid sort field: " + sortField, "SCORE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
            String sortDir = sortParts.length > 1 ? sortParts[1] : "desc";
            if ("asc".equalsIgnoreCase(sortDir)) {
                direction = Sort.Direction.ASC;
            }
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortField));

        Specification<ScoreHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userScore").get("userId"), userId));

            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (bookingId != null) {
                predicates.add(cb.equal(root.get("bookingId"), bookingId));
            }
            if (reconciliationStatus != null) {
                predicates.add(cb.equal(root.get("reconciliationStatus"), reconciliationStatus));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ScoreHistory> historyPage = scoreHistoryRepository.findAll(spec, pageRequest);
        return historyPage.map(this::mapToHistoryItemResponse);
    }

    private AdminScoreHistoryItemResponse mapToHistoryItemResponse(ScoreHistory history) {
        Long referenceHistoryId = history.getReferenceHistory() != null ? history.getReferenceHistory().getId() : null;
        String recStatusStr = history.getReconciliationStatus() != null ? history.getReconciliationStatus().name() : "NONE";
        String typeStr = history.getTransactionType() != null ? history.getTransactionType().name() : null;

        AdminScoreHistoryItemResponse response = new AdminScoreHistoryItemResponse(
                history.getId(),
                history.getEventId(),
                history.getBookingId(),
                history.getActualPointChange(),
                history.getRequestedPointChange(),
                history.getOutstandingAfter(),
                recStatusStr,
                typeStr,
                history.getBalanceBefore(),
                history.getBalanceAfter(),
                history.getAccumulatedBefore(),
                history.getAccumulatedAfter(),
                referenceHistoryId,
                history.getOperatorId(),
                history.getRequestId(),
                history.getReason(),
                history.getDescription(),
                history.getOccurredAt()
        );
        response.setHeldBefore(history.getHeldBefore());
        response.setHeldAfter(history.getHeldAfter());
        response.setOutstandingBefore(history.getOutstandingBefore());
        response.setOutstandingAfter(history.getOutstandingAfter());
        response.setTierSnapshot(history.getTierSnapshot());
        response.setEarningRateSnapshot(history.getEarningRateSnapshot());
        response.setRedeemRateSnapshot(history.getRedeemRateSnapshot());
        response.setSourceService(history.getSourceService());
        response.setCorrelationId(history.getCorrelationId());
        response.setCaseId(history.getCaseId());
        response.setApprovalId(history.getApprovalId());
        return response;
    }
}

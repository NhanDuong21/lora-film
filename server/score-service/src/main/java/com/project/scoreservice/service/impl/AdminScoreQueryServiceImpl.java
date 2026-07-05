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
        MembershipTierResponse currentTier = new MembershipTierResponse(ct.getId(), ct.getTierName(), ct.getMinPoints(), ct.getEarningRate());

        Optional<MembershipTier> nextTierOpt = membershipTierRepository.findFirstByMinPointsGreaterThanOrderByMinPointsAsc(userScore.getAccumulatedPoints());
        NextTierResponse nextTier = null;
        if (nextTierOpt.isPresent()) {
            MembershipTier nt = nextTierOpt.get();
            int pointsReq = nt.getMinPoints() - userScore.getAccumulatedPoints();
            nextTier = new NextTierResponse(nt.getId(), nt.getTierName(), nt.getMinPoints(), pointsReq);
        }

        return new AdminUserScoreResponse(
                userScore.getUserId(),
                userScore.getCurrentPoints(),
                userScore.getAccumulatedPoints(),
                currentTier,
                nextTier,
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
        // Validate pagination inputs
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessException("Invalid page or size parameters", "SCORE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

        // Validate sorting inputs
        if (sort == null || !sort.contains(",")) {
            throw new BusinessException("Invalid sort format", "SCORE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        String sortDir = sortParts.length > 1 ? sortParts[1] : "desc";

        Set<String> whitelist = Set.of(
                "createdAt", "pointChange", "transactionType", "bookingId",
                "balanceAfter", "accumulatedAfter", "outstandingPoints", "reconciliationStatus"
        );
        if (!whitelist.contains(sortField)) {
            throw new BusinessException("Invalid sort field: " + sortField, "SCORE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
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
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
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

        return new AdminScoreHistoryItemResponse(
                history.getId(),
                history.getEventId(),
                history.getBookingId(),
                history.getPointChange(),
                history.getRequestedPointChange(),
                history.getOutstandingPoints(),
                recStatusStr,
                typeStr,
                history.getBalanceBefore(),
                history.getBalanceAfter(),
                history.getAccumulatedBefore(),
                history.getAccumulatedAfter(),
                referenceHistoryId,
                history.getCreatedBy(),
                history.getRequestId(),
                history.getReason(),
                history.getDescription(),
                history.getCreatedAt()
        );
    }
}

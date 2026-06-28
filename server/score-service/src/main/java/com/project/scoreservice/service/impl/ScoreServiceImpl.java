package com.project.scoreservice.service.impl;
 
import com.project.scoreservice.client.BookingContext;
import com.project.scoreservice.client.BookingInternalClient;
import com.project.scoreservice.dto.*;
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
import com.project.scoreservice.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.validation.ConstraintViolationException;
 
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
 
@Service
public class ScoreServiceImpl implements ScoreService {
 
    private final UserScoreRepository userScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final MembershipTierService membershipTierService;
    private final BookingInternalClient bookingInternalClient;
 
    @org.springframework.beans.factory.annotation.Value("${score.redemption.value-per-point:1000}")
    private int valuePerPoint;
 
    @Autowired
    @Lazy
    private ScoreServiceImpl selfProxy;
 
    public ScoreServiceImpl(UserScoreRepository userScoreRepository,
                            ScoreHistoryRepository scoreHistoryRepository,
                            MembershipTierRepository membershipTierRepository,
                            MembershipTierService membershipTierService,
                            BookingInternalClient bookingInternalClient) {
        this.userScoreRepository = userScoreRepository;
        this.scoreHistoryRepository = scoreHistoryRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.membershipTierService = membershipTierService;
        this.bookingInternalClient = bookingInternalClient;
    }
 
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserScore createDefaultUserScore(Long userId, MembershipTier defaultTier) {
        UserScore newScore = new UserScore();
        newScore.setUserId(userId);
        newScore.setCurrentPoints(0);
        newScore.setAccumulatedPoints(0);
        newScore.setCurrentTier(defaultTier);
        return userScoreRepository.saveAndFlush(newScore);
    }
 
    @Transactional
    public UserScore getOrCreateUserScoreEntity(Long userId) {
        Optional<UserScore> existing = userScoreRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        MembershipTier defaultTier = membershipTierRepository.findFirstByOrderByMinPointsAsc()
                .orElseThrow(() -> new BusinessException("Membership tier configuration is invalid", "SCORE_TIER_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR));
        
        try {
            return selfProxy.createDefaultUserScore(userId, defaultTier);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            // Already created concurrently by another thread, fetch it
            return userScoreRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));
        }
    }
 
    @Override
    @Transactional(readOnly = true)
    public UserScoreResponse getUserScore(Long userId) {
        UserScore userScore = getOrCreateUserScoreEntity(userId);
        return mapToUserScoreResponse(userScore);
    }
 
    @Override
    @Transactional(readOnly = true)
    public UserTierResponse getUserTier(Long userId) {
        UserScore userScore = getOrCreateUserScoreEntity(userId);
        
        MembershipTier currentTier = membershipTierService.findTierForPoints(userScore.getAccumulatedPoints());
        Optional<MembershipTier> nextTierOpt = membershipTierRepository.findFirstByMinPointsGreaterThanOrderByMinPointsAsc(userScore.getAccumulatedPoints());
        
        NextTierResponse nextTier = null;
        if (nextTierOpt.isPresent()) {
            MembershipTier nt = nextTierOpt.get();
            int pointsReq = nt.getMinPoints() - userScore.getAccumulatedPoints();
            nextTier = new NextTierResponse(nt.getId(), nt.getTierName(), nt.getMinPoints(), pointsReq);
        }
 
        return new UserTierResponse(
                currentTier.getId(),
                currentTier.getTierName(),
                currentTier.getMinPoints(),
                currentTier.getEarningRate(),
                userScore.getAccumulatedPoints(),
                nextTier
        );
    }
 
    @Override
    @Transactional(readOnly = true)
    public RedeemPreviewResponse previewRedeem(Long userId, RedeemPreviewRequest request) {
        UserScore userScore = getOrCreateUserScoreEntity(userId);
        
        if (request.getRequestedPoints() <= 0) {
            throw new BusinessException("Points must be greater than zero", "SCORE_INVALID_POINT_AMOUNT", HttpStatus.BAD_REQUEST);
        }
 
        if (userScore.getCurrentPoints() < request.getRequestedPoints()) {
            throw new BusinessException("Insufficient score balance", "SCORE_INSUFFICIENT_BALANCE", HttpStatus.CONFLICT);
        }
 
        // Resolve Booking Context and validate status/ownership/expiry
        BookingContext booking = bookingInternalClient.getBookingContext(request.getBookingId());
        if (booking == null) {
            throw new BusinessException("Booking not found or booking service is unavailable", "BOOKING_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        }
 
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("Booking ownership mismatch", "SCORE_BOOKING_OWNERSHIP_MISMATCH", HttpStatus.FORBIDDEN);
        }
 
        if (!booking.isRedeemAllowed() || !"PENDING_PAYMENT".equalsIgnoreCase(booking.getStatus())) {
            throw new BusinessException("Booking is not eligible for redemption", "SCORE_BOOKING_NOT_ELIGIBLE", HttpStatus.CONFLICT);
        }
 
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Booking has expired", "SCORE_BOOKING_NOT_ELIGIBLE", HttpStatus.CONFLICT);
        }
 
        int redeemValue = request.getRequestedPoints() * valuePerPoint;
        return new RedeemPreviewResponse(request.getBookingId(), userScore.getCurrentPoints(), request.getRequestedPoints(), redeemValue);
    }
 
    @Override
    @Transactional(readOnly = true)
    public Page<ScoreHistoryResponse> getUserHistory(Long userId, int page, int size, ScoreTransactionType transactionType, Long bookingId, LocalDateTime from, LocalDateTime to, String sort) {
        if (page < 0) {
            throw new BusinessException("Page index must not be negative", "SCORE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > 50) {
            throw new BusinessException("Page size must be between 1 and 50", "SCORE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("From date cannot be after to date", "SCORE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }
 
        // Build sort order and validate whitelist
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        List<String> allowedFields = List.of("createdAt", "pointChange", "transactionType", "bookingId", "balanceAfter", "accumulatedAfter");
        if (!allowedFields.contains(sortField)) {
            throw new BusinessException("Sort field '" + sortField + "' is not allowed", "SCORE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }
 
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
 
        // Build dynamic specification
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
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
 
        Page<ScoreHistory> springPage = scoreHistoryRepository.findAll(spec, pageable);
        return springPage.map(this::mapToHistoryResponse);
    }
 
    @Override
    @Transactional(readOnly = true)
    public InternalUserScoreResponse getInternalUserScore(Long userId) {
        UserScore userScore = getOrCreateUserScoreEntity(userId);
        MembershipTier ct = membershipTierService.findTierForPoints(userScore.getAccumulatedPoints());
        
        return new InternalUserScoreResponse(
                userScore.getUserId(),
                userScore.getCurrentPoints(),
                userScore.getAccumulatedPoints(),
                ct.getTierName(),
                ct.getEarningRate()
        );
    }
 
    private UserScoreResponse mapToUserScoreResponse(UserScore userScore) {
        MembershipTier ct = membershipTierService.findTierForPoints(userScore.getAccumulatedPoints());
        MembershipTierResponse currentTier = new MembershipTierResponse(ct.getId(), ct.getTierName(), ct.getMinPoints(), ct.getEarningRate());
        
        Optional<MembershipTier> nextTierOpt = membershipTierRepository.findFirstByMinPointsGreaterThanOrderByMinPointsAsc(userScore.getAccumulatedPoints());
        NextTierResponse nextTier = null;
        if (nextTierOpt.isPresent()) {
            MembershipTier nt = nextTierOpt.get();
            int pointsReq = nt.getMinPoints() - userScore.getAccumulatedPoints();
            nextTier = new NextTierResponse(nt.getId(), nt.getTierName(), nt.getMinPoints(), pointsReq);
        }
 
        return new UserScoreResponse(
                userScore.getUserId(),
                userScore.getCurrentPoints(),
                userScore.getAccumulatedPoints(),
                currentTier,
                nextTier
        );
    }
 
    private ScoreHistoryResponse mapToHistoryResponse(ScoreHistory history) {
        return new ScoreHistoryResponse(
                history.getId(),
                history.getEventId(),
                history.getBookingId(),
                history.getPointChange(),
                history.getTransactionType(),
                history.getBalanceBefore(),
                history.getBalanceAfter(),
                history.getAccumulatedBefore(),
                history.getAccumulatedAfter(),
                history.getReferenceHistory() != null ? history.getReferenceHistory().getId() : null,
                history.getDescription()
        );
    }
}

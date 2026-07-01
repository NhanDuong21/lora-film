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
 
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScoreServiceImpl.class);
 
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

    @Override
    public ScoreEarnResponse earnScore(ScoreEarnRequest request) {
        long startTime = System.currentTimeMillis();

        Optional<ScoreHistory> historyByEvent = scoreHistoryRepository.findByEventId(request.getEventId());
        Optional<ScoreHistory> historyByIdem = scoreHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey());

        if (historyByEvent.isPresent() || historyByIdem.isPresent()) {
            ScoreHistory existing = historyByEvent.orElseGet(historyByIdem::get);

            if (!existing.getUserScore().getUserId().equals(request.getUserId())
                    || !existing.getBookingId().equals(request.getBookingId())) {
                throw new BusinessException("Idempotency conflict: event or key is already used for another request context", 
                        "SCORE_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT);
            }

            MembershipTier prevTier = membershipTierService.findTierForPoints(existing.getAccumulatedBefore());
            MembershipTier currTier = membershipTierService.findTierForPoints(existing.getAccumulatedAfter());

            log.info("Idempotent earn request processed: eventId={}, userId={}, bookingId={}", 
                    request.getEventId(), request.getUserId(), request.getBookingId());

            return new ScoreEarnResponse(
                    existing.getPointChange(),
                    existing.getBalanceBefore(),
                    existing.getBalanceAfter(),
                    existing.getAccumulatedBefore(),
                    existing.getAccumulatedAfter(),
                    prevTier.getTierName(),
                    currTier.getTierName(),
                    !prevTier.getTierName().equals(currTier.getTierName()),
                    true
            );
        }

        try {
            ScoreEarnResponse response = selfProxy.executeEarnScore(request);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Earn score result: userId={}, bookingId={}, eventId={}, earnedPoints={}, historyId={}, previousTier={}, currentTier={}, tierChanged={}, idempotent={}, duration={}ms",
                    request.getUserId(), request.getBookingId(), request.getEventId(), response.getPointChange(),
                    response.getPointChange() > 0 ? "generated" : "none", response.getPreviousTier(), response.getCurrentTier(),
                    response.getTierChanged(), false, duration);
            return response;
        } catch (org.springframework.dao.DataIntegrityViolationException | jakarta.persistence.PersistenceException e) {
            Optional<ScoreHistory> retryByEvent = scoreHistoryRepository.findByEventId(request.getEventId());
            Optional<ScoreHistory> retryByIdem = scoreHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (retryByEvent.isPresent() || retryByIdem.isPresent()) {
                ScoreHistory existing = retryByEvent.orElseGet(retryByIdem::get);
                if (existing.getUserScore().getUserId().equals(request.getUserId())
                        && existing.getBookingId().equals(request.getBookingId())) {
                    MembershipTier prevTier = membershipTierService.findTierForPoints(existing.getAccumulatedBefore());
                    MembershipTier currTier = membershipTierService.findTierForPoints(existing.getAccumulatedAfter());
                    
                    log.info("Handled concurrent unique constraint race for idempotency: eventId={}, userId={}, bookingId={}", 
                            request.getEventId(), request.getUserId(), request.getBookingId());
                            
                    return new ScoreEarnResponse(
                            existing.getPointChange(),
                            existing.getBalanceBefore(),
                            existing.getBalanceAfter(),
                            existing.getAccumulatedBefore(),
                            existing.getAccumulatedAfter(),
                            prevTier.getTierName(),
                            currTier.getTierName(),
                            !prevTier.getTierName().equals(currTier.getTierName()),
                            true
                    );
                }
            }
            throw e;
        }
    }

    @Transactional
    public ScoreEarnResponse executeEarnScore(ScoreEarnRequest request) {
        UserScore userScore = userScoreRepository.findWithLockByUserId(request.getUserId())
                .orElseGet(() -> {
                    getOrCreateUserScoreEntity(request.getUserId());
                    return userScoreRepository.findWithLockByUserId(request.getUserId())
                            .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));
                });

        MembershipTier previousTier = membershipTierService.findTierForPoints(userScore.getAccumulatedPoints());

        BigDecimal eligibleAmount = request.getEligibleAmount();
        BigDecimal earningRate = previousTier.getEarningRate();
        BigDecimal divisor = new BigDecimal("1000");

        BigDecimal result = eligibleAmount.multiply(earningRate).divide(divisor, 0, RoundingMode.FLOOR);
        
        long earnedLong = result.longValue();
        if (earnedLong < 0 || earnedLong > Integer.MAX_VALUE) {
            throw new BusinessException("Point value exceeds limit", "SCORE_POINT_OVERFLOW", HttpStatus.BAD_REQUEST);
        }
        int earnedPoints = (int) earnedLong;

        int balanceBefore = userScore.getCurrentPoints();
        int accumulatedBefore = userScore.getAccumulatedPoints();

        long newCurrent = (long) balanceBefore + earnedPoints;
        long newAccumulated = (long) accumulatedBefore + earnedPoints;

        if (newCurrent > Integer.MAX_VALUE || newAccumulated > Integer.MAX_VALUE) {
            throw new BusinessException("Point value exceeds limit", "SCORE_POINT_OVERFLOW", HttpStatus.BAD_REQUEST);
        }

        int balanceAfter = (int) newCurrent;
        int accumulatedAfter = (int) newAccumulated;

        userScore.setCurrentPoints(balanceAfter);
        userScore.setAccumulatedPoints(accumulatedAfter);

        MembershipTier currentTier = membershipTierService.findTierForPoints(accumulatedAfter);
        userScore.setCurrentTier(currentTier);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory history = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(request.getBookingId())
                .eventId(request.getEventId())
                .pointChange(earnedPoints)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .accumulatedBefore(accumulatedBefore)
                .accumulatedAfter(accumulatedAfter)
                .idempotencyKey(request.getIdempotencyKey())
                .description("Earned " + earnedPoints + " points from booking " + request.getBookingId())
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(java.util.UUID.randomUUID().toString())
                .build();

        scoreHistoryRepository.saveAndFlush(history);

        boolean tierChanged = !previousTier.getTierName().equals(currentTier.getTierName());

        return new ScoreEarnResponse(
                earnedPoints,
                balanceBefore,
                balanceAfter,
                accumulatedBefore,
                accumulatedAfter,
                previousTier.getTierName(),
                currentTier.getTierName(),
                tierChanged,
                false
        );
    }

    @Override
    public ScoreRedeemResponse redeemScore(ScoreRedeemRequest request) {
        long startTime = System.currentTimeMillis();

        Optional<ScoreHistory> historyByEvent = scoreHistoryRepository.findByEventId(request.getEventId());
        Optional<ScoreHistory> historyByIdem = scoreHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey());

        if (historyByEvent.isPresent() || historyByIdem.isPresent()) {
            ScoreHistory existing = historyByEvent.orElseGet(historyByIdem::get);

            if (!existing.getUserScore().getUserId().equals(request.getUserId())
                    || !existing.getBookingId().equals(request.getBookingId())) {
                throw new BusinessException("Idempotency conflict: event or key is already used for another request context", 
                        "SCORE_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT);
            }

            int redeemedPoints = -existing.getPointChange();
            log.info("Idempotent redeem request processed: eventId={}, userId={}, bookingId={}", 
                    request.getEventId(), request.getUserId(), request.getBookingId());

            return new ScoreRedeemResponse(
                    existing.getUserScore().getUserId(),
                    existing.getBookingId(),
                    redeemedPoints,
                    redeemedPoints * valuePerPoint,
                    existing.getBalanceAfter(),
                    existing.getAccumulatedAfter(),
                    existing.getId(),
                    true
            );
        }

        try {
            ScoreRedeemResponse response = selfProxy.executeRedeemScore(request);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Redeem score result: userId={}, bookingId={}, eventId={}, redeemedPoints={}, historyId={}, idempotent={}, duration={}ms",
                    request.getUserId(), request.getBookingId(), request.getEventId(), response.getRedeemedPoints(),
                    response.getHistoryId(), false, duration);
            return response;
        } catch (org.springframework.dao.DataIntegrityViolationException | jakarta.persistence.PersistenceException e) {
            Optional<ScoreHistory> retryByEvent = scoreHistoryRepository.findByEventId(request.getEventId());
            Optional<ScoreHistory> retryByIdem = scoreHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (retryByEvent.isPresent() || retryByIdem.isPresent()) {
                ScoreHistory existing = retryByEvent.orElseGet(retryByIdem::get);
                if (existing.getUserScore().getUserId().equals(request.getUserId())
                        && existing.getBookingId().equals(request.getBookingId())) {
                    int redeemedPoints = -existing.getPointChange();
                    log.info("Handled concurrent unique constraint race for redeem idempotency: eventId={}, userId={}, bookingId={}", 
                            request.getEventId(), request.getUserId(), request.getBookingId());
                    return new ScoreRedeemResponse(
                            existing.getUserScore().getUserId(),
                            existing.getBookingId(),
                            redeemedPoints,
                            redeemedPoints * valuePerPoint,
                            existing.getBalanceAfter(),
                            existing.getAccumulatedAfter(),
                            existing.getId(),
                            true
                    );
                }
            }
            throw e;
        }
    }

    @Transactional
    public ScoreRedeemResponse executeRedeemScore(ScoreRedeemRequest request) {
        UserScore userScore = userScoreRepository.findWithLockByUserId(request.getUserId())
                .orElseGet(() -> {
                    getOrCreateUserScoreEntity(request.getUserId());
                    return userScoreRepository.findWithLockByUserId(request.getUserId())
                            .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));
                });

        int balanceBefore = userScore.getCurrentPoints();
        int requestedPoints = request.getPoints();

        if (balanceBefore < requestedPoints) {
            java.util.Map<String, Object> data = java.util.Map.of(
                    "availablePoints", balanceBefore,
                    "requestedPoints", requestedPoints
            );
            throw new BusinessException("Insufficient score balance", "SCORE_INSUFFICIENT_BALANCE", HttpStatus.CONFLICT, data);
        }

        int balanceAfter = balanceBefore - requestedPoints;
        int accumulatedPoints = userScore.getAccumulatedPoints();

        userScore.setCurrentPoints(balanceAfter);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory history = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(request.getBookingId())
                .eventId(request.getEventId())
                .pointChange(-requestedPoints)
                .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .accumulatedBefore(accumulatedPoints)
                .accumulatedAfter(accumulatedPoints)
                .idempotencyKey(request.getIdempotencyKey())
                .description("Redeemed " + requestedPoints + " points for booking " + request.getBookingId())
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(java.util.UUID.randomUUID().toString())
                .build();

        scoreHistoryRepository.saveAndFlush(history);

        int redeemValue = requestedPoints * valuePerPoint;

        return new ScoreRedeemResponse(
                request.getUserId(),
                request.getBookingId(),
                requestedPoints,
                redeemValue,
                balanceAfter,
                accumulatedPoints,
                history.getId(),
                false
        );
    }

    @Override
    public ScoreRefundResponse refundRedeem(ScoreRefundRequest request) {
        long startTime = System.currentTimeMillis();

        Optional<ScoreHistory> historyByEvent = scoreHistoryRepository.findByEventId(request.getEventId());
        Optional<ScoreHistory> historyByIdem = scoreHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey());

        if (historyByEvent.isPresent() || historyByIdem.isPresent()) {
            ScoreHistory existing = historyByEvent.orElseGet(historyByIdem::get);

            if (!existing.getUserScore().getUserId().equals(request.getUserId())
                    || !existing.getBookingId().equals(request.getBookingId())) {
                throw new BusinessException("Idempotency conflict: event or key is already used for another request context", 
                        "SCORE_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT);
            }

            log.info("Idempotent refund request processed: eventId={}, userId={}, bookingId={}", 
                    request.getEventId(), request.getUserId(), request.getBookingId());

            return new ScoreRefundResponse(
                    existing.getUserScore().getUserId(),
                    existing.getBookingId(),
                    existing.getPointChange(),
                    existing.getBalanceAfter(),
                    existing.getAccumulatedAfter(),
                    existing.getReferenceHistory() != null ? existing.getReferenceHistory().getId() : null,
                    existing.getId(),
                    true
            );
        }

        try {
            ScoreRefundResponse response = selfProxy.executeRefundRedeem(request);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Refund score result: userId={}, bookingId={}, eventId={}, refundedPoints={}, historyId={}, idempotent={}, duration={}ms",
                    request.getUserId(), request.getBookingId(), request.getEventId(), response.getRefundedPoints(),
                    response.getHistoryId(), false, duration);
            return response;
        } catch (org.springframework.dao.DataIntegrityViolationException | jakarta.persistence.PersistenceException e) {
            Optional<ScoreHistory> retryByEvent = scoreHistoryRepository.findByEventId(request.getEventId());
            Optional<ScoreHistory> retryByIdem = scoreHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (retryByEvent.isPresent() || retryByIdem.isPresent()) {
                ScoreHistory existing = retryByEvent.orElseGet(retryByIdem::get);
                if (existing.getUserScore().getUserId().equals(request.getUserId())
                        && existing.getBookingId().equals(request.getBookingId())) {
                    log.info("Handled concurrent unique constraint race for refund idempotency: eventId={}, userId={}, bookingId={}", 
                            request.getEventId(), request.getUserId(), request.getBookingId());
                    return new ScoreRefundResponse(
                            existing.getUserScore().getUserId(),
                            existing.getBookingId(),
                            existing.getPointChange(),
                            existing.getBalanceAfter(),
                            existing.getAccumulatedAfter(),
                            existing.getReferenceHistory() != null ? existing.getReferenceHistory().getId() : null,
                            existing.getId(),
                            true
                    );
                }
            }
            throw e;
        }
    }

    @Transactional
    public ScoreRefundResponse executeRefundRedeem(ScoreRefundRequest request) {
        UserScore userScore = userScoreRepository.findWithLockByUserId(request.getUserId())
                .orElseGet(() -> {
                    getOrCreateUserScoreEntity(request.getUserId());
                    return userScoreRepository.findWithLockByUserId(request.getUserId())
                            .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));
                });

        // Locate original redeem transaction
        ScoreHistory originalRedeem = scoreHistoryRepository.findByEventId(request.getOriginalRedeemEventId())
                .orElseThrow(() -> new BusinessException("Original redeem transaction not found", "SCORE_ORIGINAL_TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (originalRedeem.getTransactionType() != ScoreTransactionType.REDEEM_FOR_BOOKING) {
            throw new BusinessException("Original transaction is not a redeem transaction", "SCORE_INVALID_TRANSACTION_TYPE", HttpStatus.BAD_REQUEST);
        }

        if (!originalRedeem.getUserScore().getUserId().equals(request.getUserId())
                || !originalRedeem.getBookingId().equals(request.getBookingId())) {
            throw new BusinessException("Original transaction user or booking mismatch", "SCORE_TRANSACTION_MISMATCH", HttpStatus.BAD_REQUEST);
        }

        int originalRedeemedPoints = -originalRedeem.getPointChange();
        if (request.getPoints() > originalRedeemedPoints) {
            throw new BusinessException("Refund points exceeds originally redeemed points", "SCORE_INVALID_REFUND_AMOUNT", HttpStatus.BAD_REQUEST);
        }

        // Prevent double refund
        boolean alreadyRefunded = scoreHistoryRepository.existsByReferenceHistoryAndTransactionType(originalRedeem, ScoreTransactionType.REFUND_REDEEM);
        if (alreadyRefunded) {
            throw new BusinessException("Redeem transaction has already been refunded", "SCORE_ALREADY_REFUNDED", HttpStatus.CONFLICT);
        }

        int balanceBefore = userScore.getCurrentPoints();
        int refundPoints = request.getPoints();
        int balanceAfter = balanceBefore + refundPoints;
        int accumulatedPoints = userScore.getAccumulatedPoints();

        userScore.setCurrentPoints(balanceAfter);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory history = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(request.getBookingId())
                .eventId(request.getEventId())
                .pointChange(refundPoints)
                .transactionType(ScoreTransactionType.REFUND_REDEEM)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .accumulatedBefore(accumulatedPoints)
                .accumulatedAfter(accumulatedPoints)
                .idempotencyKey(request.getIdempotencyKey())
                .referenceHistory(originalRedeem)
                .reason(request.getReason())
                .description("Refunded " + refundPoints + " points from cancelled booking " + request.getBookingId())
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(java.util.UUID.randomUUID().toString())
                .build();

        scoreHistoryRepository.saveAndFlush(history);

        return new ScoreRefundResponse(
                request.getUserId(),
                request.getBookingId(),
                refundPoints,
                balanceAfter,
                accumulatedPoints,
                originalRedeem.getId(),
                history.getId(),
                false
        );
    }

    @Override
    public ScoreRevokeResponse revokeEarn(ScoreRevokeRequest request) {
        long startTime = System.currentTimeMillis();

        Optional<ScoreHistory> historyByEvent = scoreHistoryRepository.findByEventId(request.getEventId());
        Optional<ScoreHistory> historyByIdem = scoreHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey());

        if (historyByEvent.isPresent() || historyByIdem.isPresent()) {
            ScoreHistory existing = historyByEvent.orElseGet(historyByIdem::get);

            if (!existing.getUserScore().getUserId().equals(request.getUserId())
                    || !existing.getBookingId().equals(request.getBookingId())
                    || !existing.getRequestedPointChange().equals(request.getPoints())
                    || existing.getReferenceHistory() == null
                    || !existing.getReferenceHistory().getEventId().equals(request.getOriginalEarnEventId())) {
                throw new BusinessException("Idempotency conflict: event or key is already used for another request context", 
                        "SCORE_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT);
            }

            log.info("Idempotent revoke request processed: eventId={}, userId={}, bookingId={}", 
                    request.getEventId(), request.getUserId(), request.getBookingId());

            return new ScoreRevokeResponse(
                    existing.getUserScore().getUserId(),
                    existing.getBookingId(),
                    existing.getRequestedPointChange(),
                    -existing.getPointChange(),
                    existing.getOutstandingPoints(),
                    existing.getBalanceAfter(),
                    existing.getAccumulatedAfter(),
                    existing.getId(),
                    existing.getReconciliationStatus().name(),
                    existing.getReconciliationStatus() == ReconciliationStatus.PENDING,
                    true
            );
        }

        try {
            ScoreRevokeResponse response = selfProxy.executeRevokeEarn(request);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Revoke score result: userId={}, bookingId={}, eventId={}, deductedPoints={}, historyId={}, idempotent={}, duration={}ms",
                    request.getUserId(), request.getBookingId(), request.getEventId(), response.getDeductedPoints(),
                    response.getHistoryId(), false, duration);
            return response;
        } catch (org.springframework.dao.DataIntegrityViolationException | jakarta.persistence.PersistenceException e) {
            Optional<ScoreHistory> retryByEvent = scoreHistoryRepository.findByEventId(request.getEventId());
            Optional<ScoreHistory> retryByIdem = scoreHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (retryByEvent.isPresent() || retryByIdem.isPresent()) {
                ScoreHistory existing = retryByEvent.orElseGet(retryByIdem::get);
                if (existing.getUserScore().getUserId().equals(request.getUserId())
                        && existing.getBookingId().equals(request.getBookingId())
                        && existing.getRequestedPointChange().equals(request.getPoints())
                        && existing.getReferenceHistory() != null
                        && existing.getReferenceHistory().getEventId().equals(request.getOriginalEarnEventId())) {
                    log.info("Handled concurrent unique constraint race for revoke idempotency: eventId={}, userId={}, bookingId={}", 
                            request.getEventId(), request.getUserId(), request.getBookingId());
                    return new ScoreRevokeResponse(
                            existing.getUserScore().getUserId(),
                            existing.getBookingId(),
                            existing.getRequestedPointChange(),
                            -existing.getPointChange(),
                            existing.getOutstandingPoints(),
                            existing.getBalanceAfter(),
                            existing.getAccumulatedAfter(),
                            existing.getId(),
                            existing.getReconciliationStatus().name(),
                            existing.getReconciliationStatus() == ReconciliationStatus.PENDING,
                            true
                    );
                }
            }
            throw e;
        }
    }

    @Transactional
    public ScoreRevokeResponse executeRevokeEarn(ScoreRevokeRequest request) {
        UserScore userScore = userScoreRepository.findWithLockByUserId(request.getUserId())
                .orElseGet(() -> {
                    getOrCreateUserScoreEntity(request.getUserId());
                    return userScoreRepository.findWithLockByUserId(request.getUserId())
                            .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));
                });

        ScoreHistory originalEarn = scoreHistoryRepository.findByEventId(request.getOriginalEarnEventId())
                .orElseThrow(() -> new BusinessException("Original earn transaction not found", "SCORE_ORIGINAL_TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (originalEarn.getTransactionType() != ScoreTransactionType.EARN_BY_BOOKING) {
            throw new BusinessException("Original transaction is not an earn transaction", "SCORE_INVALID_TRANSACTION_TYPE", HttpStatus.BAD_REQUEST);
        }

        if (!originalEarn.getUserScore().getUserId().equals(request.getUserId())
                || !originalEarn.getBookingId().equals(request.getBookingId())) {
            throw new BusinessException("Original transaction user or booking mismatch", "SCORE_TRANSACTION_MISMATCH", HttpStatus.BAD_REQUEST);
        }

        List<ScoreHistory> previousRevokes = scoreHistoryRepository.findByReferenceHistoryAndTransactionType(originalEarn, ScoreTransactionType.REVOKE_EARN_BY_REFUND);
        int totalPreviousRevokesRequested = previousRevokes.stream().mapToInt(ScoreHistory::getRequestedPointChange).sum();
        int originalEarnAmount = originalEarn.getPointChange();
        int remainingRevokable = originalEarnAmount - totalPreviousRevokesRequested;

        if (remainingRevokable <= 0) {
            throw new BusinessException("Original transaction has already been fully revoked", "SCORE_ALREADY_REVOKED", HttpStatus.BAD_REQUEST);
        }

        if (request.getPoints() > remainingRevokable) {
            throw new BusinessException("Revoke points exceeds originally earned points", "SCORE_INVALID_REVOKE_AMOUNT", HttpStatus.BAD_REQUEST);
        }

        int balanceBefore = userScore.getCurrentPoints();
        int requestedPoints = request.getPoints();
        int actualDeducted = Math.min(balanceBefore, requestedPoints);
        int outstandingPoints = requestedPoints - actualDeducted;
        ReconciliationStatus reconciliationStatus = outstandingPoints > 0 ? ReconciliationStatus.PENDING : ReconciliationStatus.NONE;

        int balanceAfter = balanceBefore - actualDeducted;
        int accumulatedBefore = userScore.getAccumulatedPoints();
        if (accumulatedBefore - requestedPoints < 0) {
            log.warn("User ID {} has abnormal accumulated points deduction. Current accumulated: {}, requested deduction: {}",
                    request.getUserId(), accumulatedBefore, requestedPoints);
        }
        int accumulatedAfter = Math.max(0, accumulatedBefore - requestedPoints);

        userScore.setCurrentPoints(balanceAfter);
        userScore.setAccumulatedPoints(accumulatedAfter);

        MembershipTier currentTier = membershipTierService.findTierForPoints(accumulatedAfter);
        userScore.setCurrentTier(currentTier);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory history = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(request.getBookingId())
                .eventId(request.getEventId())
                .pointChange(-actualDeducted)
                .transactionType(ScoreTransactionType.REVOKE_EARN_BY_REFUND)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .accumulatedBefore(accumulatedBefore)
                .accumulatedAfter(accumulatedAfter)
                .idempotencyKey(request.getIdempotencyKey())
                .referenceHistory(originalEarn)
                .reason(request.getReason())
                .description("Revoked " + actualDeducted + " points (requested: " + requestedPoints + ") for booking " + request.getBookingId())
                .requestedPointChange(requestedPoints)
                .outstandingPoints(outstandingPoints)
                .reconciliationStatus(reconciliationStatus)
                .requestId(java.util.UUID.randomUUID().toString())
                .build();

        scoreHistoryRepository.saveAndFlush(history);

        return new ScoreRevokeResponse(
                request.getUserId(),
                request.getBookingId(),
                requestedPoints,
                actualDeducted,
                outstandingPoints,
                balanceAfter,
                accumulatedAfter,
                history.getId(),
                reconciliationStatus.name(),
                reconciliationStatus == ReconciliationStatus.PENDING,
                false
        );
    }
}

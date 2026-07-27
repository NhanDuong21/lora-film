package com.project.scoreservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.dto.*;
import com.project.scoreservice.entity.*;
import com.project.scoreservice.enumtype.*;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.*;
import com.project.scoreservice.service.AdminScoreOperationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminScoreOperationServiceImpl implements AdminScoreOperationService {

    private final UserScoreRepository userScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final ScoreHoldRepository scoreHoldRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final MembershipTierHistoryRepository membershipTierHistoryRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationDetailRepository reconciliationDetailRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AdminScoreOperationServiceImpl(UserScoreRepository userScoreRepository,
                                          ScoreHistoryRepository scoreHistoryRepository,
                                          ScoreHoldRepository scoreHoldRepository,
                                          MembershipTierRepository membershipTierRepository,
                                          MembershipTierHistoryRepository membershipTierHistoryRepository,
                                          ReconciliationRunRepository reconciliationRunRepository,
                                          ReconciliationDetailRepository reconciliationDetailRepository,
                                          AuditLogRepository auditLogRepository,
                                          ObjectMapper objectMapper) {
        this.userScoreRepository = userScoreRepository;
        this.scoreHistoryRepository = scoreHistoryRepository;
        this.scoreHoldRepository = scoreHoldRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.membershipTierHistoryRepository = membershipTierHistoryRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationDetailRepository = reconciliationDetailRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AdminAdjustmentResponse adjustScore(Long userId, ScoreAdjustmentRequest request, String operatorId, String clientIp) {
        Long effectiveUserId = userId != null ? userId : request.userId();
        if (effectiveUserId == null) {
            throw new BusinessException("User ID is required for score adjustment", "SCORE_USER_ID_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        ScoreAdjustmentType type = request.getEffectiveType();
        if (type == null) {
            throw new BusinessException("Adjustment type is required", "SCORE_ADJUSTMENT_TYPE_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (request.points() == null || request.points() <= 0) {
            throw new BusinessException("Points must be greater than zero", "SCORE_INVALID_POINT_AMOUNT", HttpStatus.BAD_REQUEST);
        }
        if (request.reason() == null || request.reason().trim().isEmpty()) {
            throw new BusinessException("Reason is required for manual score adjustment", "SCORE_REASON_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        long opId = parseOperatorId(operatorId);

        // Idempotency check
        if (request.requestId() != null && !request.requestId().trim().isEmpty()) {
            Optional<ScoreHistory> existingOpt = scoreHistoryRepository.findByRequestId(request.requestId().trim());
            if (existingOpt.isPresent()) {
                ScoreHistory existing = existingOpt.get();
                boolean typeMatches = (type == ScoreAdjustmentType.ADD && existing.getTransactionType() == ScoreTransactionType.MANUAL_ADD)
                        || (type == ScoreAdjustmentType.DEDUCT && existing.getTransactionType() == ScoreTransactionType.MANUAL_DEDUCT);
                boolean pointsMatch = Math.abs(existing.getPointChange()) == request.points().intValue();
                if (typeMatches && pointsMatch && existing.getUserScore().getUserId().equals(effectiveUserId)) {
                    saveAuditLog(opId, effectiveUserId, existing.getId(), "ACTION_MANUAL_ADJUSTMENT_IDEMPOTENT", "USER_SCORE_" + effectiveUserId, 200, clientIp, request, null, 0L);
                    return new AdminAdjustmentResponse(
                            effectiveUserId,
                            type.name(),
                            existing.getPointChange(),
                            existing.getBalanceAfter(),
                            existing.getAccumulatedAfter(),
                            existing.getTierSnapshot(),
                            existing.getTierSnapshot(),
                            false,
                            existing.getId(),
                            true
                    );
                } else {
                    throw new BusinessException("Idempotency conflict: different payload for same request ID", "SCORE_ADJUSTMENT_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT);
                }
            }
        }

        UserScore userScore = userScoreRepository.findWithLockByUserId(effectiveUserId)
                .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (userScore.getStatus() == UserScoreStatus.LOCKED) {
            throw new BusinessException("User membership account is locked", "SCORE_ACCOUNT_LOCKED", HttpStatus.FORBIDDEN);
        }

        int pointChange = (type == ScoreAdjustmentType.ADD) ? request.points() : -request.points();

        // Check balance underflow for DEDUCT
        if (type == ScoreAdjustmentType.DEDUCT) {
            if (userScore.getCurrentPoints() < request.points()) {
                if (!request.getEffectiveAllowNegative()) {
                    throw new BusinessException("Score balance would be negative after deduction", "SCORE_BALANCE_WOULD_BE_NEGATIVE", HttpStatus.CONFLICT);
                }
            }
        }

        // Check overflow for ADD
        if (type == ScoreAdjustmentType.ADD) {
            if ((long) userScore.getCurrentPoints() + pointChange > Integer.MAX_VALUE) {
                throw new BusinessException("Point overflow: balance would exceed maximum allowed integer", "SCORE_POINT_OVERFLOW", HttpStatus.BAD_REQUEST);
            }
        }

        int oldBalance = userScore.getCurrentPoints();
        int oldAccumulated = userScore.getAccumulatedPoints();
        MembershipTier oldTier = userScore.getCurrentTier();
        String previousTierCode = oldTier.getTierCode();

        userScore.setCurrentPoints(oldBalance + pointChange);
        if (request.getEffectiveAffectAccumulatedPoints()) {
            userScore.setAccumulatedPoints(oldAccumulated + pointChange);
        }

        boolean tierChanged = false;
        if (request.getEffectiveAffectAccumulatedPoints()) {
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
        }

        userScoreRepository.save(userScore);

        if (tierChanged) {
            MembershipTierHistory tierHistory = MembershipTierHistory.builder()
                    .userScore(userScore)
                    .oldTierCode(previousTierCode)
                    .newTierCode(userScore.getCurrentTier().getTierCode())
                    .reason("Upgraded tier due to manual adjustment: " + request.reason())
                    .build();
            membershipTierHistoryRepository.save(tierHistory);
        }

        String reqId = (request.requestId() != null && !request.requestId().trim().isEmpty()) ? request.requestId().trim() : "REQ-ADJ-" + UUID.randomUUID().toString();
        ScoreTransactionType txType = (type == ScoreAdjustmentType.ADD) ? ScoreTransactionType.MANUAL_ADD : ScoreTransactionType.MANUAL_DEDUCT;

        ScoreHistory history = ScoreHistory.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userScore(userScore)
                .idempotencyKey(reqId)
                .requestId(reqId)
                .sourceService("ADMIN")
                .transactionType(txType)
                .requestedPointChange(pointChange)
                .actualPointChange(pointChange)
                .balanceBefore(oldBalance)
                .balanceAfter(userScore.getCurrentPoints())
                .heldBefore(userScore.getHeldPoints())
                .heldAfter(userScore.getHeldPoints())
                .accumulatedBefore(oldAccumulated)
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .outstandingBefore(0)
                .outstandingPoints(0)
                .tierSnapshot(userScore.getCurrentTier().getTierCode())
                .reason(request.reason())
                .description("Admin manual score adjustment (" + type + "): " + request.reason())
                .operatorId(opId != 0L ? opId : null)
                .reconciliationStatus(ReconciliationStatus.NONE)
                .build();
        scoreHistoryRepository.save(history);

        AdminAdjustmentResponse response = new AdminAdjustmentResponse(
                effectiveUserId,
                type.name(),
                pointChange,
                userScore.getCurrentPoints(),
                userScore.getAccumulatedPoints(),
                userScore.getCurrentTier().getTierCode(),
                previousTierCode,
                tierChanged,
                history.getId(),
                false
        );

        saveAuditLog(opId, effectiveUserId, history.getId(), "ACTION_MANUAL_ADJUSTMENT", "USER_SCORE_" + effectiveUserId, 201, clientIp, request, response, 0L);

        return response;
    }

    @Override
    @Transactional
    public AdminAdjustmentResponse reverseAdjustment(Long userId, ReverseAdjustmentRequest request, String operatorId, String clientIp) {
        if (request == null || request.historyId() == null) {
            throw new BusinessException("History ID is required for reverse adjustment", "SCORE_HISTORY_ID_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (request.reason() == null || request.reason().trim().isEmpty()) {
            throw new BusinessException("Reason is required for reversing adjustment", "SCORE_REASON_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        long opId = parseOperatorId(operatorId);

        ScoreHistory original = scoreHistoryRepository.findById(request.historyId())
                .orElseThrow(() -> new BusinessException("Original transaction not found", "SCORE_HISTORY_NOT_FOUND", HttpStatus.NOT_FOUND));

        Long effectiveUserId = original.getUserScore().getUserId();
        if (userId != null && !userId.equals(effectiveUserId)) {
            throw new BusinessException("User ID does not match original transaction", "SCORE_USER_ID_MISMATCH", HttpStatus.BAD_REQUEST);
        }

        if (scoreHistoryRepository.existsByReferenceHistoryAndTransactionType(original, ScoreTransactionType.REVERSE_ADJUSTMENT)) {
            throw new BusinessException("Transaction has already been reversed", "SCORE_ALREADY_REVERSED", HttpStatus.CONFLICT);
        }

        UserScore userScore = userScoreRepository.findWithLockByUserId(effectiveUserId)
                .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        int reverseChange = -original.getActualPointChange();

        if (reverseChange < 0 && userScore.getCurrentPoints() + reverseChange < 0) {
            throw new BusinessException("Score balance would be negative after reversal", "SCORE_BALANCE_WOULD_BE_NEGATIVE", HttpStatus.CONFLICT);
        }

        int oldBalance = userScore.getCurrentPoints();
        int oldAccumulated = userScore.getAccumulatedPoints();
        String oldTierCode = userScore.getCurrentTier().getTierCode();

        userScore.setCurrentPoints(oldBalance + reverseChange);
        userScoreRepository.save(userScore);

        String reqId = (request.requestId() != null && !request.requestId().trim().isEmpty()) ? request.requestId().trim() : "REQ-REV-" + UUID.randomUUID().toString();

        ScoreHistory history = ScoreHistory.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userScore(userScore)
                .referenceHistory(original)
                .idempotencyKey(reqId)
                .requestId(reqId)
                .sourceService("ADMIN")
                .transactionType(ScoreTransactionType.REVERSE_ADJUSTMENT)
                .requestedPointChange(reverseChange)
                .actualPointChange(reverseChange)
                .balanceBefore(oldBalance)
                .balanceAfter(userScore.getCurrentPoints())
                .heldBefore(userScore.getHeldPoints())
                .heldAfter(userScore.getHeldPoints())
                .accumulatedBefore(oldAccumulated)
                .accumulatedAfter(userScore.getAccumulatedPoints())
                .outstandingBefore(0)
                .outstandingPoints(0)
                .tierSnapshot(oldTierCode)
                .reason(request.reason())
                .description("Reversed transaction #" + original.getId() + ": " + request.reason())
                .operatorId(opId != 0L ? opId : null)
                .reconciliationStatus(ReconciliationStatus.NONE)
                .build();
        scoreHistoryRepository.save(history);

        AdminAdjustmentResponse response = new AdminAdjustmentResponse(
                effectiveUserId,
                ScoreTransactionType.REVERSE_ADJUSTMENT.name(),
                reverseChange,
                userScore.getCurrentPoints(),
                userScore.getAccumulatedPoints(),
                userScore.getCurrentTier().getTierCode(),
                oldTierCode,
                false,
                history.getId(),
                false
        );

        saveAuditLog(opId, effectiveUserId, history.getId(), "ACTION_REVERSE_ADJUSTMENT", "USER_SCORE_" + effectiveUserId, 201, clientIp, request, response, 0L);

        return response;
    }

    @Override
    @Transactional
    public AdminAdjustmentResponse recalculateTier(Long userId, String operatorId, String clientIp) {
        if (userId == null) {
            throw new BusinessException("User ID is required", "SCORE_USER_ID_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        long opId = parseOperatorId(operatorId);

        UserScore userScore = userScoreRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new BusinessException("User score account not found", "SCORE_USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        MembershipTier oldTier = userScore.getCurrentTier();
        String previousTierCode = oldTier.getTierCode();

        List<MembershipTier> allTiers = membershipTierRepository.findAll();
        MembershipTier newTier = oldTier;
        for (MembershipTier t : allTiers) {
            if (Boolean.TRUE.equals(t.getActive()) && userScore.getAccumulatedPoints() >= t.getMinAccumulatedPoints()) {
                if (t.getMinAccumulatedPoints() >= newTier.getMinAccumulatedPoints()) {
                    newTier = t;
                }
            }
        }

        boolean tierChanged = !newTier.getTierCode().equals(previousTierCode);
        if (tierChanged) {
            userScore.setCurrentTier(newTier);
            userScoreRepository.save(userScore);

            MembershipTierHistory tierHistory = MembershipTierHistory.builder()
                    .userScore(userScore)
                    .oldTierCode(previousTierCode)
                    .newTierCode(newTier.getTierCode())
                    .reason("Admin triggered tier recalculation")
                    .build();
            membershipTierHistoryRepository.save(tierHistory);
        }

        AdminAdjustmentResponse response = new AdminAdjustmentResponse(
                userId,
                "RECALCULATE_TIER",
                0,
                userScore.getCurrentPoints(),
                userScore.getAccumulatedPoints(),
                userScore.getCurrentTier().getTierCode(),
                previousTierCode,
                tierChanged,
                null,
                false
        );

        saveAuditLog(opId, userId, null, "ACTION_RECALCULATE_TIER", "USER_SCORE_" + userId, 200, clientIp, null, response, 0L);

        return response;
    }

    @Override
    @Transactional
    public ReconciliationDTOs.ReconciliationRunResponse runReconciliation(ReconciliationDTOs.ReconciliationRunRequest request, String operatorId) {
        long opId = parseOperatorId(operatorId);

        if (request != null && request.batchCode() != null && !request.batchCode().trim().isEmpty()) {
            Optional<ReconciliationRun> existing = reconciliationRunRepository.findByBatchCode(request.batchCode().trim());
            if (existing.isPresent()) {
                return ReconciliationDTOs.ReconciliationRunResponse.fromEntity(existing.get());
            }
        }

        String batchCode = (request != null && request.batchCode() != null && !request.batchCode().trim().isEmpty())
                ? request.batchCode().trim()
                : "RECON-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        ReconciliationRun run = new ReconciliationRun(batchCode, ReconciliationRunStatus.RUNNING, LocalDateTime.now(), opId, request != null ? request.remark() : "Manual reconciliation job");
        reconciliationRunRepository.save(run);

        List<UserScore> allUsers = userScoreRepository.findAll();
        int totalUsers = allUsers.size();
        int matched = 0;
        int mismatched = 0;

        for (UserScore us : allUsers) {
            Integer ledgerBal = scoreHistoryRepository.sumActualPointChangeByUserId(us.getUserId());
            int calcBalance = ledgerBal != null ? ledgerBal : 0;
            int balDiff = us.getCurrentPoints() - calcBalance;

            Integer ledgerHeld = scoreHoldRepository.sumActiveHeldPointsByUserId(us.getUserId());
            int calcHeld = ledgerHeld != null ? ledgerHeld : 0;
            int heldDiff = us.getHeldPoints() - calcHeld;

            Integer ledgerAccum = scoreHistoryRepository.sumEarnedPointsByUserId(us.getUserId());
            int calcAccum = ledgerAccum != null ? ledgerAccum : 0;
            int accumDiff = us.getAccumulatedPoints() - calcAccum;

            ReconciliationDetail detail = new ReconciliationDetail();
            detail.setRun(run);
            detail.setUserId(us.getUserId());
            detail.setCurrentBalance(us.getCurrentPoints());
            detail.setLedgerBalance(calcBalance);
            detail.setBalanceDifference(balDiff);
            detail.setCurrentHeldPoints(us.getHeldPoints());
            detail.setLedgerHeldPoints(calcHeld);
            detail.setHeldDifference(heldDiff);
            detail.setCurrentAccumulated(us.getAccumulatedPoints());
            detail.setLedgerAccumulated(calcAccum);
            detail.setAccumulatedDifference(accumDiff);

            if (balDiff == 0 && heldDiff == 0 && accumDiff == 0) {
                detail.setStatus(ReconciliationDetailStatus.MATCHED);
                detail.setRemark("Balance and ledger match");
                matched++;
            } else {
                detail.setStatus(ReconciliationDetailStatus.MISMATCH);
                detail.setRemark("Discrepancy detected: BalDiff=" + balDiff + ", HeldDiff=" + heldDiff + ", AccumDiff=" + accumDiff);
                mismatched++;
            }
            reconciliationDetailRepository.save(detail);
        }

        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setFinishedAt(LocalDateTime.now());
        run.setTotalUsers(totalUsers);
        run.setMatchedUsers(matched);
        run.setMismatchedUsers(mismatched);
        reconciliationRunRepository.save(run);

        saveAuditLog(opId, null, run.getId(), "ACTION_RUN_RECONCILIATION", "RECONCILIATION_RUN_" + run.getId(), 200, null, request, ReconciliationDTOs.ReconciliationRunResponse.fromEntity(run), 0L);

        return ReconciliationDTOs.ReconciliationRunResponse.fromEntity(run);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReconciliationDTOs.ReconciliationRunResponse> getReconciliationRuns(int page, int size, ReconciliationRunStatus status, LocalDateTime from, LocalDateTime to) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        Page<ReconciliationRun> springPage;
        if (status != null) {
            springPage = reconciliationRunRepository.findByStatus(status, pageable);
        } else if (from != null && to != null) {
            springPage = reconciliationRunRepository.findByStartedAtBetween(from, to, pageable);
        } else {
            springPage = reconciliationRunRepository.findAll(pageable);
        }
        Page<ReconciliationDTOs.ReconciliationRunResponse> mapped = springPage.map(ReconciliationDTOs.ReconciliationRunResponse::fromEntity);
        return new PageResponse<>(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReconciliationDTOs.ReconciliationDetailResponse> getReconciliationDetails(Long runId, int page, int size, ReconciliationDetailStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<ReconciliationDetail> springPage;
        if (runId != null && status != null) {
            springPage = reconciliationDetailRepository.findByRunIdAndStatus(runId, status, pageable);
        } else if (runId != null) {
            springPage = reconciliationDetailRepository.findByRunId(runId, pageable);
        } else {
            springPage = reconciliationDetailRepository.findAll(pageable);
        }
        Page<ReconciliationDTOs.ReconciliationDetailResponse> mapped = springPage.map(ReconciliationDTOs.ReconciliationDetailResponse::fromEntity);
        return new PageResponse<>(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTOs.AuditLogResponse> getAuditLogs(int page, int size, Long userId, Long operatorId, String action, LocalDateTime from, LocalDateTime to) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> springPage = auditLogRepository.findByFilters(userId, operatorId, action, from, to, pageable);
        Page<AuditLogDTOs.AuditLogResponse> mapped = springPage.map(AuditLogDTOs.AuditLogResponse::fromEntity);
        return new PageResponse<>(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportScoreData(String type, String format, Long userId, LocalDateTime from, LocalDateTime to) {
        StringBuilder csv = new StringBuilder();
        // Add UTF-8 BOM for Excel support
        csv.append("\uFEFF");

        if ("RECONCILIATION".equalsIgnoreCase(type)) {
            csv.append("ID,Run ID,User ID,Current Balance,Ledger Balance,Difference,Status,Remark,Created At\n");
            List<ReconciliationDetail> details = reconciliationDetailRepository.findAll();
            for (ReconciliationDetail d : details) {
                csv.append(String.format("%d,%d,%d,%d,%d,%d,\"%s\",\"%s\",\"%s\"\n",
                        d.getId(),
                        d.getRun() != null ? d.getRun().getId() : 0,
                        d.getUserId(),
                        d.getCurrentBalance(),
                        d.getLedgerBalance(),
                        d.getBalanceDifference(),
                        d.getStatus().name(),
                        escapeCsv(d.getRemark()),
                        d.getCreatedAt() != null ? d.getCreatedAt().toString() : ""
                ));
            }
        } else if ("AUDIT".equalsIgnoreCase(type)) {
            csv.append("ID,Operator ID,User ID,Action,Resource,HTTP Status,Client IP,Created At\n");
            List<AuditLog> logs = auditLogRepository.findAllByFilters(userId, null, null, from, to);
            for (AuditLog l : logs) {
                csv.append(String.format("%d,%s,%s,\"%s\",\"%s\",%s,\"%s\",\"%s\"\n",
                        l.getId(),
                        l.getOperatorId() != null ? l.getOperatorId().toString() : "",
                        l.getUserId() != null ? l.getUserId().toString() : "",
                        escapeCsv(l.getAction()),
                        escapeCsv(l.getResource()),
                        l.getHttpStatus() != null ? l.getHttpStatus().toString() : "",
                        escapeCsv(l.getClientIp()),
                        l.getCreatedAt() != null ? l.getCreatedAt().toString() : ""
                ));
            }
        } else {
            // Default: SCORE HISTORY
            csv.append("ID,User ID,Transaction Type,Point Change,Balance After,Occurred At,Reason,Description\n");
            List<ScoreHistory> histories;
            if (userId != null) {
                histories = scoreHistoryRepository.findAll((root, query, cb) -> cb.equal(root.get("userScore").get("userId"), userId));
            } else {
                histories = scoreHistoryRepository.findAll(Sort.by("occurredAt").descending());
            }
            for (ScoreHistory h : histories) {
                csv.append(String.format("%d,%d,\"%s\",%d,%d,\"%s\",\"%s\",\"%s\"\n",
                        h.getId(),
                        h.getUserScore() != null ? h.getUserScore().getUserId() : 0,
                        h.getTransactionType().name(),
                        h.getActualPointChange() != null ? h.getActualPointChange() : 0,
                        h.getBalanceAfter() != null ? h.getBalanceAfter() : 0,
                        h.getOccurredAt() != null ? h.getOccurredAt().toString() : "",
                        escapeCsv(h.getReason()),
                        escapeCsv(h.getDescription())
                ));
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreDashboardResponse getDashboardStats() {
        long totalMembers = userScoreRepository.count();
        long silver = membershipTierRepository.findByTierCode("SILVER").map(userScoreRepository::countByCurrentTier).orElse(0L);
        long gold = membershipTierRepository.findByTierCode("GOLD").map(userScoreRepository::countByCurrentTier).orElse(0L);
        long diamond = membershipTierRepository.findByTierCode("DIAMOND").map(userScoreRepository::countByCurrentTier).orElse(0L);

        long earned = 0;
        long redeemed = 0;
        long expired = 0;
        long held = 0;

        List<UserScore> allUsers = userScoreRepository.findAll();
        for (UserScore u : allUsers) {
            held += u.getHeldPoints();
            Integer userEarn = scoreHistoryRepository.sumEarnedPointsByUserId(u.getUserId());
            if (userEarn != null) earned += userEarn;
        }

        long pendingRecon = reconciliationDetailRepository.countByRunIdAndStatus(
                reconciliationRunRepository.findAll(Sort.by("id").descending()).stream().findFirst().map(ReconciliationRun::getId).orElse(-1L),
                ReconciliationDetailStatus.MISMATCH
        );

        Optional<ReconciliationRun> lastRun = reconciliationRunRepository.findAll(Sort.by("id").descending()).stream().findFirst();

        return new ScoreDashboardResponse(
                totalMembers,
                earned,
                redeemed,
                held,
                expired,
                silver,
                gold,
                diamond,
                pendingRecon,
                lastRun.map(ReconciliationRun::getBatchCode).orElse("NONE"),
                lastRun.map(ReconciliationRun::getStartedAt).orElse(null)
        );
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    private long parseOperatorId(String operatorId) {
        if (operatorId == null || operatorId.trim().isEmpty()) return 0L;
        try {
            return Long.parseLong(operatorId.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void saveAuditLog(Long operatorId, Long userId, Long historyId, String action, String resource, Integer status, String clientIp, Object reqPayload, Object respPayload, Long durationMs) {
        try {
            AuditLog log = new AuditLog();
            log.setTransactionUuid(UUID.randomUUID().toString());
            log.setOperatorId(operatorId != null && operatorId != 0L ? operatorId : null);
            log.setUserId(userId);
            log.setHistoryId(historyId);
            log.setAction(action);
            log.setResource(resource);
            log.setHttpStatus(status);
            log.setClientIp(clientIp);
            if (reqPayload != null) {
                try {
                    log.setRequestPayload(objectMapper.writeValueAsString(reqPayload));
                } catch (JsonProcessingException ignored) {}
            }
            if (respPayload != null) {
                try {
                    log.setResponsePayload(objectMapper.writeValueAsString(respPayload));
                } catch (JsonProcessingException ignored) {}
            }
            log.setDurationMs(durationMs);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Never let audit log failure break the business transaction
        }
    }
}

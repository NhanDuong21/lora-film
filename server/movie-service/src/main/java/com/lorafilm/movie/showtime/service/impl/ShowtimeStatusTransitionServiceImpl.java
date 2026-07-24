package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeMapper;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.dto.response.BatchStatusActionSummary;
import com.lorafilm.movie.showtime.dto.response.BatchStatusBlockedShowtime;
import com.lorafilm.movie.showtime.dto.response.BatchStatusReasonGroup;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShowtimeStatusTransitionServiceImpl implements ShowtimeStatusTransitionService {

    private static final int REASON_GROUP_SAMPLE_SIZE = 5;

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeStatusHistoryService historyService;
    private final CurrentUserProvider currentUserProvider;
    private final AdminShowtimeMapper adminShowtimeMapper;
    private final Clock clock;
    private final ShowtimePricingService showtimePricingService;

    public ShowtimeStatusTransitionServiceImpl(ShowtimeRepository showtimeRepository,
                                               ShowtimeStatusHistoryService historyService,
                                               CurrentUserProvider currentUserProvider,
                                               AdminShowtimeMapper adminShowtimeMapper,
                                               Clock clock,
                                               ShowtimePricingService showtimePricingService) {
        this.showtimeRepository = showtimeRepository;
        this.historyService = historyService;
        this.currentUserProvider = currentUserProvider;
        this.adminShowtimeMapper = adminShowtimeMapper;
        this.clock = clock;
        this.showtimePricingService = showtimePricingService;
    }

    @Override
    @Transactional
    public AdminShowtimeResponse transitionStatus(String showtimePublicId, UpdateShowtimeStatusRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_AVAILABLE, "Current user not available");
        }

        Showtime showtime = showtimeRepository.findByPublicIdForUpdate(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        Instant now = Instant.now(clock);
        Showtime savedShowtime = applyTransition(showtime, request.getStatus(), request.getReason(), currentUserId, now);

        return adminShowtimeMapper.toAdminResponse(savedShowtime);
    }

    private Showtime applyTransition(Showtime showtime,
                                     ShowtimeStatus newStatus,
                                     String rawReason,
                                     Long currentUserId,
                                     Instant now) {
        ShowtimeStatus currentStatus = showtime.getStatus();
        validateTransitionMatrix(currentStatus, newStatus);
        String reason = normalizeReason(rawReason, newStatus);
        validateTimingRules(showtime, currentStatus, newStatus, now);
        applyTimestamps(showtime, currentStatus, newStatus, now);

        if (newStatus == ShowtimeStatus.CANCELLED) {
            showtime.setCancellationReason(reason);
        }

        showtime.setStatus(newStatus);
        Showtime savedShowtime = showtimeRepository.saveAndFlush(showtime);
        historyService.recordTransitionHistory(
                savedShowtime, currentStatus, newStatus, reason, currentUserId, now);
        return savedShowtime;
    }

    private void validateTransitionMatrix(ShowtimeStatus current, ShowtimeStatus target) {
        boolean valid = false;
        switch (current) {
            case DRAFT:
                valid = (target == ShowtimeStatus.OPEN_FOR_BOOKING || target == ShowtimeStatus.CANCELLED);
                break;
            case OPEN_FOR_BOOKING:
                valid = (target == ShowtimeStatus.CLOSED || target == ShowtimeStatus.CANCELLED);
                break;
            case CLOSED:
                valid = (target == ShowtimeStatus.FINISHED || target == ShowtimeStatus.CANCELLED);
                break;
            case CANCELLED:
            case FINISHED:
                valid = false;
                break;
        }

        if (!valid) {
            throw new BusinessException(ErrorCode.INVALID_SHOWTIME_STATUS_TRANSITION,
                    "Invalid transition from " + current + " to " + target);
        }
    }

    private String normalizeReason(String rawReason, ShowtimeStatus target) {
        String reason = rawReason == null ? null : rawReason.trim();
        if (reason != null && reason.isEmpty()) {
            reason = null;
        }

        if (target == ShowtimeStatus.CANCELLED && reason == null) {
            throw new BusinessException(ErrorCode.SHOWTIME_CANCELLATION_REASON_REQUIRED, "Cancellation reason is required");
        }

        return reason;
    }

    private void validateTimingRules(Showtime showtime, ShowtimeStatus current, ShowtimeStatus target, Instant now) {
        if (current == ShowtimeStatus.DRAFT && target == ShowtimeStatus.OPEN_FOR_BOOKING) {
            if (!showtime.getStartTime().isAfter(now)) {
                throw new BusinessException(ErrorCode.SHOWTIME_CANNOT_OPEN_AFTER_START, "Cannot open showtime for booking after it has started");
            }
            showtimePricingService.validateCompleteness(showtime);
        }
        
        if (current == ShowtimeStatus.CLOSED && target == ShowtimeStatus.FINISHED) {
            if (now.isBefore(showtime.getEndTime())) {
                throw new BusinessException(ErrorCode.SHOWTIME_CANNOT_FINISH_BEFORE_END, "Cannot finish showtime before it ends");
            }
        }
    }

    private void applyTimestamps(Showtime showtime, ShowtimeStatus current, ShowtimeStatus target, Instant now) {
        if (current == ShowtimeStatus.DRAFT && target == ShowtimeStatus.OPEN_FOR_BOOKING) {
            if (showtime.getBookingOpenTime() == null) {
                showtime.setBookingOpenTime(now);
            }
        } else if (current == ShowtimeStatus.OPEN_FOR_BOOKING && (target == ShowtimeStatus.CLOSED || target == ShowtimeStatus.CANCELLED)) {
            if (showtime.getBookingCloseTime() == null) {
                showtime.setBookingCloseTime(now);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BatchStatusActionSummary previewBatchStatus(String batchId, ShowtimeStatus targetStatus) {
        validateBatchRequest(batchId, targetStatus);
        List<Showtime> showtimes =
                showtimeRepository.findAllByBatchIdAndDeletedAtIsNullOrderByIdAsc(batchId);
        ensureBatchExists(batchId, showtimes);

        Instant now = Instant.now(clock);
        BatchClassification classification = classifyBatch(batchId, showtimes, targetStatus, now);
        classification.summary().setActorId(currentUserProvider.getCurrentUserId());
        classification.summary().setActionAt(now);
        return classification.summary();
    }

    @Override
    @Transactional
    public BatchStatusActionSummary transitionBatchStatus(String batchId, UpdateShowtimeStatusRequest request) {
        ShowtimeStatus targetStatus = request == null ? null : request.getStatus();
        validateBatchRequest(batchId, targetStatus);

        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_AVAILABLE, "Current user not available");
        }

        List<Showtime> showtimes = showtimeRepository.findAllByBatchIdForUpdate(batchId);
        ensureBatchExists(batchId, showtimes);

        Instant now = Instant.now(clock);
        BatchClassification classification = classifyBatch(batchId, showtimes, targetStatus, now);
        BatchStatusActionSummary summary = classification.summary();
        summary.setActorId(currentUserId);
        summary.setActionAt(now);

        if (!summary.isActionAllowed()) {
            return summary;
        }

        for (Showtime showtime : classification.eligible()) {
            applyTransition(showtime, targetStatus, request.getReason(), currentUserId, now);
        }
        summary.setAffectedCount(classification.eligible().size());
        return summary;
    }

    private void validateBatchRequest(String batchId, ShowtimeStatus targetStatus) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Batch ID is required");
        }
        if (targetStatus == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Target status is required");
        }
        if (targetStatus == ShowtimeStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.SHOWTIME_BATCH_CANCELLATION_SAFETY_UNAVAILABLE,
                    "Batch cancellation is disabled until booking safety can be verified");
        }
        if (targetStatus != ShowtimeStatus.OPEN_FOR_BOOKING) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Only OPEN_FOR_BOOKING is supported for batch status actions");
        }
    }

    private void ensureBatchExists(String batchId, List<Showtime> showtimes) {
        if (showtimes.isEmpty()) {
            throw new ResourceNotFoundException("No showtimes found for batch ID: " + batchId);
        }
    }

    private BatchClassification classifyBatch(String batchId,
                                              List<Showtime> showtimes,
                                              ShowtimeStatus targetStatus,
                                              Instant now) {
        List<Showtime> eligible = new ArrayList<>();
        List<BatchStatusBlockedShowtime> blockedShowtimes = new ArrayList<>();
        int alreadyTargetCount = 0;
        Map<String, ReasonAccumulator> reasons = new LinkedHashMap<>();

        for (Showtime showtime : showtimes) {
            if (showtime.getStatus() == targetStatus) {
                alreadyTargetCount++;
                continue;
            }

            try {
                validateTransitionMatrix(showtime.getStatus(), targetStatus);
                validateTimingRules(showtime, showtime.getStatus(), targetStatus, now);
                eligible.add(showtime);
            } catch (BusinessException exception) {
                ErrorCode errorCode = exception.getErrorCode();
                if (errorCode == null) {
                    throw exception;
                }
                String code = errorCode.name();
                String safeReason = errorCode.getMessage();
                reasons.computeIfAbsent(
                        code,
                        ignored -> new ReasonAccumulator(safeReason))
                        .add(showtime.getPublicId());
                blockedShowtimes.add(new BatchStatusBlockedShowtime(
                        showtime.getPublicId(), code, safeReason));
            }
        }

        BatchStatusActionSummary summary = new BatchStatusActionSummary();
        summary.setBatchId(batchId);
        summary.setTargetStatus(targetStatus.name());
        summary.setTotalCount(showtimes.size());
        summary.setEligibleCount(eligible.size());
        summary.setAlreadyTargetCount(alreadyTargetCount);
        summary.setSkippedCount(showtimes.size() - eligible.size() - alreadyTargetCount);
        summary.setFailedCount(0);
        summary.setAffectedCount(0);
        summary.setAtomic(true);
        summary.setActionAllowed(summary.getSkippedCount() == 0);
        summary.setReasonGroups(reasons.entrySet().stream()
                .map(entry -> new BatchStatusReasonGroup(
                        entry.getKey(),
                        entry.getValue().reason(),
                        entry.getValue().count(),
                        entry.getValue().sampleShowtimePublicIds()))
                .toList());
        summary.setBlockedShowtimes(List.copyOf(blockedShowtimes));
        return new BatchClassification(summary, eligible);
    }

    private record BatchClassification(
            BatchStatusActionSummary summary,
            List<Showtime> eligible) {
    }

    private static final class ReasonAccumulator {
        private final String reason;
        private final List<String> sampleShowtimePublicIds = new ArrayList<>();
        private int count;

        private ReasonAccumulator(String reason) {
            this.reason = reason;
        }

        private void add(String showtimePublicId) {
            count++;
            if (showtimePublicId != null
                    && sampleShowtimePublicIds.size() < REASON_GROUP_SAMPLE_SIZE) {
                sampleShowtimePublicIds.add(showtimePublicId);
            }
        }

        private String reason() {
            return reason;
        }

        private int count() {
            return count;
        }

        private List<String> sampleShowtimePublicIds() {
            return List.copyOf(sampleShowtimePublicIds);
        }
    }
}

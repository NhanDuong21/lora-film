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
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeSpecification;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ShowtimeStatusTransitionServiceImpl implements ShowtimeStatusTransitionService {

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

        ShowtimeStatus currentStatus = showtime.getStatus();
        ShowtimeStatus newStatus = request.getStatus();

        validateTransitionMatrix(currentStatus, newStatus);
        
        String reason = normalizeReason(request.getReason(), newStatus);
        Instant now = Instant.now(clock);

        validateTimingRules(showtime, currentStatus, newStatus, now);

        applyTimestamps(showtime, currentStatus, newStatus, now);
        
        if (newStatus == ShowtimeStatus.CANCELLED) {
            showtime.setCancellationReason(reason);
        }

        showtime.setStatus(newStatus);
        showtime = showtimeRepository.saveAndFlush(showtime);

        historyService.recordTransitionHistory(showtime, currentStatus, newStatus, reason, currentUserId, now);

        return adminShowtimeMapper.toAdminResponse(showtime);
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
    @Transactional
    public void transitionBatchStatus(String batchId, UpdateShowtimeStatusRequest request) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Batch ID is required");
        }

        Specification<Showtime> spec = ShowtimeSpecification.hasBatchId(batchId);
        List<Showtime> showtimes = showtimeRepository.findAll(spec);
        
        if (showtimes.isEmpty()) {
            throw new ResourceNotFoundException("No showtimes found for batch ID: " + batchId);
        }

        for (Showtime showtime : showtimes) {
            // Re-use the existing transition logic for each item
            transitionStatus(showtime.getPublicId(), request);
        }
    }
}

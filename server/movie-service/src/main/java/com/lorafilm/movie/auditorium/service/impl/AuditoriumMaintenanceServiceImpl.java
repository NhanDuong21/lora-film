package com.lorafilm.movie.auditorium.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.domain.enums.MaintenanceType;
import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.ExtendMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceImpactResponse;
import com.lorafilm.movie.auditorium.dto.MaintenanceWindowResponse;
import com.lorafilm.movie.auditorium.dto.ResolveMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceImpactService;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceService;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuditoriumMaintenanceServiceImpl implements AuditoriumMaintenanceService {

    private final AuditoriumMaintenanceWindowRepository maintenanceRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final AuditoriumMaintenanceImpactService impactService;
    private final ShowtimeStatusTransitionService showtimeTransitionService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public AuditoriumMaintenanceServiceImpl(
            AuditoriumMaintenanceWindowRepository maintenanceRepository,
            AuditoriumRepository auditoriumRepository,
            AuditoriumMaintenanceImpactService impactService,
            ShowtimeStatusTransitionService showtimeTransitionService,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.maintenanceRepository = maintenanceRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.impactService = impactService;
        this.showtimeTransitionService = showtimeTransitionService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse createWindow(
            String auditoriumPublicId,
            CreateMaintenanceWindowRequest request) {
        Auditorium auditorium = auditoriumRepository
                .findByPublicIdAndDeletedAtIsNullForUpdate(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        Instant now = Instant.now(clock);
        MaintenanceType maintenanceType = request.maintenanceType() == null
                ? MaintenanceType.PLANNED
                : request.maintenanceType();
        Instant startTime = maintenanceType == MaintenanceType.EMERGENCY
                ? now
                : request.startTime();
        validateCreateTimeRange(startTime, request.endTime(), maintenanceType, now);

        Optional<AuditoriumMaintenanceWindow> overlap = maintenanceRepository.findFirstOverlap(
                auditorium.getId(), ActionStatus.ACTIVE, startTime, request.endTime());
        if (overlap.isPresent()) {
            throw overlapException(overlap.get());
        }

        CreateMaintenanceWindowRequest normalizedRequest = new CreateMaintenanceWindowRequest(
                startTime,
                request.endTime(),
                request.reason().trim(),
                maintenanceType);
        MaintenanceImpactResponse impact = impactService.preview(auditoriumPublicId, normalizedRequest);
        if (maintenanceType == MaintenanceType.PLANNED
                && (impact.affectedShowtimeCount() > 0 || !impact.bookingDataComplete())) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("affectedShowtimeCount", impact.affectedShowtimeCount());
            errorData.put("openForBookingCount", impact.openForBookingCount());
            errorData.put("occupiedSeatCount", impact.occupiedSeatCount());
            errorData.put("bookingDataComplete", impact.bookingDataComplete());
            throw new BusinessException(ErrorCode.PLANNED_MAINTENANCE_HAS_AFFECTED_SHOWTIMES, errorData);
        }

        Long currentUserId = currentUserProvider.getCurrentUserId();
        AuditoriumMaintenanceWindow window = new AuditoriumMaintenanceWindow();
        window.setAuditorium(auditorium);
        window.setStartTime(startTime);
        window.setEndTime(request.endTime());
        window.setReason(request.reason().trim());
        window.setMaintenanceType(maintenanceType);
        window.setStatus(ActionStatus.ACTIVE);
        window.setCreatedBy(currentUserId);
        window.setUpdatedBy(currentUserId);
        window = maintenanceRepository.saveAndFlush(window);

        if (maintenanceType == MaintenanceType.EMERGENCY) {
            closeAffectedShowtimesForEmergency(window, impact);
        }
        return mapToResponse(window);
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse cancelWindow(Long maintenanceWindowId) {
        AuditoriumMaintenanceWindow window = findWindowForUpdate(maintenanceWindowId);
        requireActive(window);
        if (!Instant.now(clock).isBefore(window.getStartTime())) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_CANNOT_BE_CANCELLED_AFTER_START);
        }

        window.setStatus(ActionStatus.CANCELLED);
        window.setUpdatedBy(currentUserProvider.getCurrentUserId());
        return mapToResponse(window);
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse resolveWindow(
            Long maintenanceWindowId,
            ResolveMaintenanceWindowRequest request) {
        AuditoriumMaintenanceWindow window = findWindowForUpdate(maintenanceWindowId);
        requireActive(window);
        Instant now = Instant.now(clock);
        if (now.isBefore(window.getStartTime())) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_CANNOT_BE_RESOLVED_BEFORE_START);
        }

        Long currentUserId = currentUserProvider.getCurrentUserId();
        window.setStatus(ActionStatus.RESOLVED);
        window.setActualEndTime(now);
        window.setResolvedBy(currentUserId);
        window.setResolutionNote(request.resolutionNote().trim());
        window.setUpdatedBy(currentUserId);
        return mapToResponse(window);
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse extendWindow(
            Long maintenanceWindowId,
            ExtendMaintenanceWindowRequest request) {
        AuditoriumMaintenanceWindow window = findWindowForUpdate(maintenanceWindowId);
        requireActive(window);
        Instant now = Instant.now(clock);
        if (!request.endTime().isAfter(window.getEndTime()) || !request.endTime().isAfter(now)) {
            throw new BusinessException(ErrorCode.MAINTENANCE_EXTENSION_MUST_INCREASE_END_TIME);
        }

        Optional<AuditoriumMaintenanceWindow> overlap = maintenanceRepository.findFirstOverlapExcluding(
                window.getAuditorium().getId(),
                window.getId(),
                ActionStatus.ACTIVE,
                window.getStartTime(),
                request.endTime());
        if (overlap.isPresent()) {
            throw overlapException(overlap.get());
        }

        CreateMaintenanceWindowRequest impactRequest = new CreateMaintenanceWindowRequest(
                window.getEndTime(),
                request.endTime(),
                window.getReason(),
                window.getMaintenanceType());
        MaintenanceImpactResponse impact = impactService.preview(
                window.getAuditorium().getPublicId(), impactRequest);
        if (window.getMaintenanceType() == MaintenanceType.PLANNED
                && (impact.affectedShowtimeCount() > 0 || !impact.bookingDataComplete())) {
            throw new BusinessException(ErrorCode.PLANNED_MAINTENANCE_HAS_AFFECTED_SHOWTIMES);
        }

        window.setEndTime(request.endTime());
        window.setExtensionNote(request.note().trim());
        window.setUpdatedBy(currentUserProvider.getCurrentUserId());
        if (window.getMaintenanceType() == MaintenanceType.EMERGENCY) {
            closeAffectedShowtimesForEmergency(window, impact);
        }
        return mapToResponse(window);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceWindowResponse> getMaintenanceWindows(String auditoriumPublicId) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        return maintenanceRepository.findByAuditoriumIdOrderByStartTimeDesc(auditorium.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateCreateTimeRange(
            Instant startTime,
            Instant endTime,
            MaintenanceType maintenanceType,
            Instant now) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_MAINTENANCE_TIME_RANGE);
        }
        if (maintenanceType == MaintenanceType.PLANNED && startTime.isBefore(now)) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_CANNOT_BE_CREATED_IN_PAST);
        }
    }

    private void closeAffectedShowtimesForEmergency(
            AuditoriumMaintenanceWindow window,
            MaintenanceImpactResponse impact) {
        String reason = "Tự động đóng bán do sự cố phòng chiếu #" + window.getId()
                + ": " + window.getReason();
        impact.showtimes().stream()
                .filter(showtime -> showtime.status() == ShowtimeStatus.OPEN_FOR_BOOKING)
                .forEach(showtime -> {
                    UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
                    request.setStatus(ShowtimeStatus.CLOSED);
                    request.setReason(reason);
                    showtimeTransitionService.transitionStatus(showtime.showtimePublicId(), request);
                });
    }

    private AuditoriumMaintenanceWindow findWindowForUpdate(Long maintenanceWindowId) {
        return maintenanceRepository.findByIdForUpdate(maintenanceWindowId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAINTENANCE_WINDOW_NOT_FOUND));
    }

    private void requireActive(AuditoriumMaintenanceWindow window) {
        if (window.getStatus() != ActionStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_NOT_ACTIVE);
        }
    }

    private BusinessException overlapException(AuditoriumMaintenanceWindow conflict) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("conflictingWindowId", conflict.getId());
        errorData.put("conflictingStartTime", conflict.getStartTime());
        errorData.put("conflictingEndTime", conflict.getEndTime());
        return new BusinessException(ErrorCode.MAINTENANCE_WINDOW_OVERLAPS, errorData);
    }

    private MaintenanceWindowResponse mapToResponse(AuditoriumMaintenanceWindow window) {
        return new MaintenanceWindowResponse(
                window.getId(),
                window.getAuditorium().getPublicId(),
                window.getStartTime(),
                window.getEndTime(),
                window.getReason(),
                window.getMaintenanceType(),
                window.getStatus(),
                window.getActualEndTime(),
                window.getResolvedBy(),
                window.getResolutionNote(),
                window.getExtensionNote(),
                window.getCreatedAt(),
                window.getUpdatedAt(),
                window.getCreatedBy(),
                window.getUpdatedBy());
    }
}

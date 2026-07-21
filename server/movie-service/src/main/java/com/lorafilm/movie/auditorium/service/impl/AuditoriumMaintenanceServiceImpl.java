package com.lorafilm.movie.auditorium.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceWindowResponse;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceService;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class AuditoriumMaintenanceServiceImpl implements AuditoriumMaintenanceService {

    private final AuditoriumMaintenanceWindowRepository maintenanceRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public AuditoriumMaintenanceServiceImpl(AuditoriumMaintenanceWindowRepository maintenanceRepository, AuditoriumRepository auditoriumRepository, CurrentUserProvider currentUserProvider, Clock clock) {
        this.maintenanceRepository = maintenanceRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse createWindow(String auditoriumPublicId, CreateMaintenanceWindowRequest request) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        if (request.startTime() == null || request.endTime() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Start time and end time must not be null");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            java.util.Map<String, Object> errorData = new java.util.HashMap<>();
            errorData.put("startTime", request.startTime());
            errorData.put("endTime", request.endTime());
            errorData.put("fieldErrors", java.util.List.of(
                new com.lorafilm.movie.common.api.FieldErrorDetail("endTime", request.endTime(), "endTime must be after startTime")
            ));
            throw new BusinessException(ErrorCode.INVALID_MAINTENANCE_TIME_RANGE, errorData);
        }
        if (request.startTime().isBefore(Instant.now(clock))) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_CANNOT_BE_CREATED_IN_PAST);
        }

        java.util.Optional<AuditoriumMaintenanceWindow> overlapOpt = maintenanceRepository.findFirstOverlap(auditorium.getId(), ActionStatus.ACTIVE, request.startTime(), request.endTime());
        if (overlapOpt.isPresent()) {
            AuditoriumMaintenanceWindow conflict = overlapOpt.get();
            java.util.Map<String, Object> errorData = new java.util.HashMap<>();
            errorData.put("conflictingWindowId", conflict.getId());
            errorData.put("conflictingStartTime", conflict.getStartTime());
            errorData.put("conflictingEndTime", conflict.getEndTime());
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_OVERLAPS, errorData);
        }

        AuditoriumMaintenanceWindow window = new AuditoriumMaintenanceWindow();
        window.setAuditorium(auditorium);
        window.setStartTime(request.startTime());
        window.setEndTime(request.endTime());
        window.setReason(request.reason());
        window.setStatus(ActionStatus.ACTIVE);
        window.setCreatedBy(currentUserProvider.getCurrentUserId());
        window.setUpdatedBy(currentUserProvider.getCurrentUserId());
        
        window = maintenanceRepository.save(window);
        return mapToResponse(window);
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse cancelWindow(Long maintenanceWindowId) {
        AuditoriumMaintenanceWindow window = maintenanceRepository.findByIdForUpdate(maintenanceWindowId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAINTENANCE_WINDOW_NOT_FOUND));

        if (window.getStatus() == ActionStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_ALREADY_CANCELLED);
        }

        window.setStatus(ActionStatus.CANCELLED);
        window.setUpdatedBy(currentUserProvider.getCurrentUserId());
        return mapToResponse(window);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceWindowResponse> getMaintenanceWindows(String auditoriumPublicId) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        List<AuditoriumMaintenanceWindow> windows = maintenanceRepository.findByAuditoriumIdOrderByStartTimeDesc(auditorium.getId());
        return windows.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private MaintenanceWindowResponse mapToResponse(AuditoriumMaintenanceWindow w) {
        return new MaintenanceWindowResponse(
            w.getId(),
            w.getAuditorium().getPublicId(),
            w.getStartTime(),
            w.getEndTime(),
            w.getReason(),
            w.getStatus(),
            w.getCreatedAt(),
            w.getUpdatedAt(),
            w.getCreatedBy(),
            w.getUpdatedBy()
        );
    }
}

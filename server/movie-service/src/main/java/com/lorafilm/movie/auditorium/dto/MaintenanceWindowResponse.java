package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.auditorium.domain.enums.MaintenanceType;
import com.lorafilm.movie.common.enums.ActionStatus;

import java.time.Instant;

public record MaintenanceWindowResponse(
        Long id,
        String auditoriumPublicId,
        Instant startTime,
        Instant endTime,
        String reason,
        MaintenanceType maintenanceType,
        ActionStatus status,
        Instant actualEndTime,
        Long resolvedBy,
        String resolutionNote,
        String extensionNote,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy,
        Long updatedBy
) {}

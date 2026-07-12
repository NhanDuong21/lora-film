package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.common.enums.ActionStatus;

import java.time.Instant;

public record MaintenanceWindowResponse(
        Long id,
        String auditoriumPublicId,
        Instant startTime,
        Instant endTime,
        String reason,
        ActionStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy,
        Long updatedBy
) {}

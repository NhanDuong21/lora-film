package com.lorafilm.movie.auditorium.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateMaintenanceWindowRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @Size(max = 255) String reason
) {}

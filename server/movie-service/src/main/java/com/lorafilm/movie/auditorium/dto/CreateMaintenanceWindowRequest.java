package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.auditorium.domain.enums.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateMaintenanceWindowRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotBlank(message = "Vui lòng nhập lý do bảo trì")
        @Size(max = 255, message = "Lý do bảo trì không được vượt quá 255 ký tự") String reason,
        MaintenanceType maintenanceType
) {}

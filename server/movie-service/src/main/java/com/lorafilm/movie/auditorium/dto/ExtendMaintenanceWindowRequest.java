package com.lorafilm.movie.auditorium.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ExtendMaintenanceWindowRequest(
        @NotNull(message = "Vui lòng nhập thời gian kết thúc mới")
        Instant endTime,
        @NotBlank(message = "Vui lòng nhập lý do cần thêm thời gian")
        @Size(max = 500, message = "Lý do gia hạn không được vượt quá 500 ký tự")
        String note
) {}

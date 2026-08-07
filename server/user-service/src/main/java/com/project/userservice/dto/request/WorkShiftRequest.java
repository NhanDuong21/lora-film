package com.project.userservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record WorkShiftRequest(
        @NotNull Long employeeId,
        @NotNull LocalDateTime scheduledStart,
        @NotNull LocalDateTime scheduledEnd,
        @Size(max = 150) String location,
        @Size(max = 500) String note
) {
}

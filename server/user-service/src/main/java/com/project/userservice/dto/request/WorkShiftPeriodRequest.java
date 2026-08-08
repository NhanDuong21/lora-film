package com.project.userservice.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record WorkShiftPeriodRequest(
        @NotNull LocalDateTime scheduledStart,
        @NotNull LocalDateTime scheduledEnd
) {
}

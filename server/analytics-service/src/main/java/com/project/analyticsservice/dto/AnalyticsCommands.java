package com.project.analyticsservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class AnalyticsCommands {
    private AnalyticsCommands() {
    }

    public record RecommendationStatus(
            @NotBlank @Size(max = 30) String status) {
    }

    public record RebuildJob(
            @NotBlank @Size(max = 100) String requestId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @Size(max = 30) String mode) {
    }
}

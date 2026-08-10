package com.project.analyticsservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record DemandHistorySnapshotRequest(
        @NotBlank @Size(max = 100) String cinemaPublicId,
        @NotNull LocalDate historyFrom,
        @NotNull LocalDate historyTo,
        @NotBlank @Size(max = 50) String cinemaTimezone,
        @NotNull @Size(max = 100) List<@NotBlank @Size(max = 64) String> moviePublicIds) {
}

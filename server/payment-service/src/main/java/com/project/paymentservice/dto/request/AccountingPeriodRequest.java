package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AccountingPeriodRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String periodCode,
        @Size(max = 36) String cinemaPublicId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @Size(max = 1000) String note) {
}

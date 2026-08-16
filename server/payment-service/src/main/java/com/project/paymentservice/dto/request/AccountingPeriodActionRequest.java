package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountingPeriodActionRequest(
        @NotNull Action action,
        @NotBlank @Size(min = 5, max = 1000) String note,
        @NotNull Integer expectedVersion) {
    public enum Action { RECONCILE, LOCK, REOPEN }
}

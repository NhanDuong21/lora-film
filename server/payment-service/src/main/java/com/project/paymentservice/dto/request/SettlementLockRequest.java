package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SettlementLockRequest(
        @NotNull Integer expectedVersion,
        @NotBlank @Size(min = 5, max = 1000) String note) {
}

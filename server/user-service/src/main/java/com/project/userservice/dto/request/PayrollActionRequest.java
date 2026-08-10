package com.project.userservice.dto.request;

import com.project.userservice.enumtype.PayrollActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PayrollActionRequest(
        @NotNull PayrollActionType type,
        @Size(max = 500) String reason,
        @Size(max = 100) String paymentReference,
        @Size(max = 100) String bankBatchReference,
        @Size(max = 100) String accountingReference,
        Boolean reconciliationMatched,
        @NotNull Integer expectedVersion
) {
}

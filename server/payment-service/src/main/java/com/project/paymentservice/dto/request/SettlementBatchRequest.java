package com.project.paymentservice.dto.request;

import com.project.paymentservice.enumtype.ProviderCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record SettlementBatchRequest(
        @NotNull ProviderCode providerCode,
        @NotBlank @Size(max = 100) String batchCode,
        @Size(max = 36) String cinemaPublicId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @Size(max = 255) String sourceFileName,
        @Size(max = 1000) String note,
        @NotEmpty @Size(max = 500) List<@Valid SettlementEntryRequest> entries) {
}

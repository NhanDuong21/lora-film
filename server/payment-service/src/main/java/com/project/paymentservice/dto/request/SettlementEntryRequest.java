package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SettlementEntryRequest(
        @NotBlank @Size(max = 100) String paymentTransactionCode,
        @NotBlank @Size(max = 150) String providerTransactionId,
        @Size(max = 150) String bankCreditReference,
        @NotNull @DecimalMin("0.00") BigDecimal providerGrossAmount,
        @NotNull @DecimalMin("0.00") BigDecimal providerFeeAmount,
        @NotNull @DecimalMin("0.00") BigDecimal providerNetAmount,
        @NotNull @DecimalMin("0.00") BigDecimal bankCreditAmount) {
}

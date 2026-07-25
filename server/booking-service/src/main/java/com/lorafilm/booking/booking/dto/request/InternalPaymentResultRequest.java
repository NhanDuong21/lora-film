package com.lorafilm.booking.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InternalPaymentResultRequest(
        @NotBlank String eventId,
        String schemaVersion,
        @NotNull Long paymentId,
        String paymentTransactionCode,
        String paymentMethod,
        @NotBlank String result,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        @NotBlank String currency,
        LocalDateTime occurredAt,
        String externalTransactionId,
        String reconciliationStatus
) {
}

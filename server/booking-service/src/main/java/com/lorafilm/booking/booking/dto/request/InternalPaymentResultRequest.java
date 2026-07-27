package com.lorafilm.booking.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record InternalPaymentResultRequest(
        @NotBlank
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
                message = "eventId must be a valid UUID")
        String eventId,
        @NotBlank
        @Pattern(regexp = "^1\\.0$", message = "schemaVersion must be 1.0")
        String schemaVersion,
        Long paymentId,
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
                message = "paymentPublicId must be a valid UUID")
        String paymentPublicId,
        String paymentTransactionCode,
        @Size(max = 50) String paymentProvider,
        @Size(max = 50) String paymentMethod,
        @NotBlank
        @Pattern(
                regexp = "^(SUCCESS|FAILED|CANCELLED|TIMEOUT|PENDING|REFUND_SUCCESS|REFUND_FAILED)$",
                message = "result is not supported")
        String result,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        @NotBlank String currency,
        Instant occurredAt,
        @Size(max = 100) String externalTransactionId
) {
    @AssertTrue(message = "paymentPublicId is required for canonical requests; paymentId is accepted only for compatibility")
    public boolean hasPaymentIdentity() {
        return (paymentPublicId != null && !paymentPublicId.isBlank()) || paymentId != null;
    }
}

package com.lorafilm.booking.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Paid booking spending for the authenticated customer in one calendar year")
public record BookingSpendingSummaryResponse(
        int year,
        BigDecimal totalSpending,
        String currency,
        Instant periodStart,
        Instant periodEnd) {
}

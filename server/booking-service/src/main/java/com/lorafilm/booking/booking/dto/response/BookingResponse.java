package com.lorafilm.booking.booking.dto.response;

import com.lorafilm.booking.booking.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Booking creation or status-change result")
public record BookingResponse(
        @Schema(description = "Public UUID used by APIs", example = "550e8400-e29b-41d4-a716-446655440000")
        String publicId,
        String bookingCode,
        String showtimePublicId,
        BookingStatus status,
        BigDecimal totalAmount,
        Integer scorePointsUsed,
        BigDecimal scoreDiscount,
        String currency,
        Instant expiredAt,
        Instant amountLockedAt,
        Instant createdAt) {
}

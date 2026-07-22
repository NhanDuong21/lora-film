package com.lorafilm.booking.booking.dto.response;

import com.lorafilm.booking.booking.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Compact booking item used in paginated lists")
public record BookingSummaryResponse(
        @Schema(description = "Public UUID used by APIs", example = "550e8400-e29b-41d4-a716-446655440000")
        String publicId,
        String bookingCode,
        Long showtimeId,
        BookingStatus status,
        BigDecimal totalAmount,
        String currency,
        Instant expiredAt,
        Instant createdAt) {
}

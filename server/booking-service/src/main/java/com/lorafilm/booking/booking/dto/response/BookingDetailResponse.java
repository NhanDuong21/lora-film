package com.lorafilm.booking.booking.dto.response;

import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Complete booking aggregate data")
public record BookingDetailResponse(
        @Schema(description = "Public UUID used by APIs", example = "550e8400-e29b-41d4-a716-446655440000")
        String publicId,
        String bookingCode,
        Long userId,
        Long showtimeId,
        String showtimePublicId,
        Long movieId,
        Long cinemaId,
        Long auditoriumId,
        BigDecimal ticketAmount,
        BigDecimal foodAmount,
        BigDecimal serviceFee,
        BigDecimal taxAmount,
        BigDecimal promotionDiscount,
        BigDecimal voucherDiscount,
        BigDecimal totalAmount,
        String currency,
        BookingStatus status,
        PaymentStatus paymentStatus,
        Instant paymentDeadline,
        Instant amountLockedAt,
        Instant confirmedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant expiredAt,
        Instant refundedAt,
        String cancelReasonCode,
        String cancelReasonDetail,
        String note,
        Instant createdAt,
        Instant updatedAt) {
}

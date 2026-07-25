package com.lorafilm.booking.booking.dto.response;

public record InternalPaymentResultResponse(
        Long bookingId,
        String eventId,
        String bookingStatus,
        String paymentStatus,
        boolean idempotent
) {
}

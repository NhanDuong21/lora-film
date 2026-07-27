package com.lorafilm.booking.booking.dto.response;

public record InternalPaymentResultResponse(
        Long bookingId,
        String bookingPublicId,
        Long paymentId,
        String paymentPublicId,
        String eventId,
        String bookingStatus,
        String paymentStatus,
        boolean accepted,
        boolean idempotent,
        boolean reconciliationRequired,
        String reconciliationTaskPublicId
) {
    public InternalPaymentResultResponse asIdempotentReplay() {
        return new InternalPaymentResultResponse(
                bookingId,
                bookingPublicId,
                paymentId,
                paymentPublicId,
                eventId,
                bookingStatus,
                paymentStatus,
                accepted,
                true,
                reconciliationRequired,
                reconciliationTaskPublicId);
    }
}

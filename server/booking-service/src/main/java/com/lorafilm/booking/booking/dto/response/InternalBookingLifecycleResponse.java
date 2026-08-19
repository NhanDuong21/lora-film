package com.lorafilm.booking.booking.dto.response;

/**
 * Read-only lifecycle snapshot for internal audit and incident coordination.
 * Unlike the payment context, terminal bookings are valid responses here.
 */
public record InternalBookingLifecycleResponse(
        String bookingPublicId,
        String bookingCode,
        String bookingStatus) {
}

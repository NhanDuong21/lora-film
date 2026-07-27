package com.lorafilm.booking.booking.dto;

import java.time.Instant;

public record BookingOperationalInfoDto(
        String reservationState,
        int heldSeatCount,
        int bookedSeatCount,
        int releasedSeatCount,
        int expiredSeatCount,
        boolean paymentAttempted,
        String attentionCode,
        Instant stateChangedAt,
        String reasonCode,
        String reasonDetail) {
}

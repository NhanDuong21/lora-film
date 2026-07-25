package com.lorafilm.booking.booking.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    REFUNDED;

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING_PAYMENT, EnumSet.of(CONFIRMED, CANCELLED, EXPIRED),
            CONFIRMED, EnumSet.of(COMPLETED, REFUNDED, CANCELLED),
            COMPLETED, EnumSet.noneOf(BookingStatus.class),
            CANCELLED, EnumSet.noneOf(BookingStatus.class),
            EXPIRED, EnumSet.noneOf(BookingStatus.class),
            REFUNDED, EnumSet.noneOf(BookingStatus.class)
    );

    public boolean canTransitionTo(BookingStatus targetStatus) {
        return targetStatus != null && ALLOWED_TRANSITIONS.get(this).contains(targetStatus);
    }
}

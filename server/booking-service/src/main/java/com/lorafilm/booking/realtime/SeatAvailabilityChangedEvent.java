package com.lorafilm.booking.realtime;

import java.time.Instant;
import java.util.List;

/**
 * A committed database state change that can be projected to a public
 * showtime room.  It deliberately contains public identifiers only.
 */
public record SeatAvailabilityChangedEvent(
        String showtimePublicId,
        Instant occurredAt,
        List<SeatUpdate> seats) {

    public SeatAvailabilityChangedEvent {
        seats = seats == null ? List.of() : List.copyOf(seats);
    }

    public record SeatUpdate(String seatPublicId, String status, Instant expiresAt) {
    }
}

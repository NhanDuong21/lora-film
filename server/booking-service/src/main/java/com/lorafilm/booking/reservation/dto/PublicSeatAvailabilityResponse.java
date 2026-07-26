package com.lorafilm.booking.reservation.dto;

import java.time.Instant;
import java.util.List;

public record PublicSeatAvailabilityResponse(
        String showtimePublicId,
        int maxSeatsPerBooking,
        List<OccupiedSeat> occupiedSeats) {

    public record OccupiedSeat(String seatPublicId, String status, Instant expiresAt) {
    }
}

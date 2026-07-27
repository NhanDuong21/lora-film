package com.lorafilm.booking.booking.dto;

import com.lorafilm.booking.reservation.enums.SeatReservationStatus;

import java.time.Instant;

public record BookingReservationAdminDto(
        String publicId,
        String seatPublicId,
        String seatLabel,
        String seatType,
        SeatReservationStatus status,
        Instant reservedAt,
        Instant expiresAt,
        Instant updatedAt,
        String reason) {
}

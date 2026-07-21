package com.lorafilm.booking.dto.response;

import com.lorafilm.booking.domain.enums.SeatReservationStatus;

import java.time.Instant;
import java.util.UUID;

public class SeatReservationResponse {

    private Long id;
    private UUID publicId;
    private Long showtimeId;
    private Long seatId;
    private Long userId;
    private SeatReservationStatus status;
    private UUID reservationToken;
    private Instant expiresAt;

    public SeatReservationResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SeatReservationStatus getStatus() {
        return status;
    }

    public void setStatus(SeatReservationStatus status) {
        this.status = status;
    }

    public UUID getReservationToken() {
        return reservationToken;
    }

    public void setReservationToken(UUID reservationToken) {
        this.reservationToken = reservationToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}

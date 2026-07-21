package com.lorafilm.booking.reservation.dto;

import java.time.Instant;
import java.util.List;

public class HoldSeatResponse {

    private List<Long> reservationIds;
    private List<String> reservationPublicIds;
    private Instant expiresAt;

    public HoldSeatResponse() {
    }

    public HoldSeatResponse(List<Long> reservationIds, Instant expiresAt) {
        this.reservationIds = reservationIds;
        this.expiresAt = expiresAt;
    }

    public HoldSeatResponse(List<Long> reservationIds, List<String> reservationPublicIds, Instant expiresAt) {
        this.reservationIds = reservationIds;
        this.reservationPublicIds = reservationPublicIds;
        this.expiresAt = expiresAt;
    }

    public List<Long> getReservationIds() {
        return reservationIds;
    }

    public void setReservationIds(List<Long> reservationIds) {
        this.reservationIds = reservationIds;
    }

    public List<String> getReservationPublicIds() {
        return reservationPublicIds;
    }

    public void setReservationPublicIds(List<String> reservationPublicIds) {
        this.reservationPublicIds = reservationPublicIds;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}

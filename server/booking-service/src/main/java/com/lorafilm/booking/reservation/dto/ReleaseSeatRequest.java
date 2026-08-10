package com.lorafilm.booking.reservation.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ReleaseSeatRequest {

    @NotEmpty(message = "reservationIds cannot be empty")
    private List<Long> reservationIds;

    private String reason;

    public ReleaseSeatRequest() {
    }

    public ReleaseSeatRequest(List<Long> reservationIds) {
        this.reservationIds = reservationIds;
    }

    public ReleaseSeatRequest(List<Long> reservationIds, String reason) {
        this.reservationIds = reservationIds;
        this.reason = reason;
    }

    public List<Long> getReservationIds() {
        return reservationIds;
    }

    public void setReservationIds(List<Long> reservationIds) {
        this.reservationIds = reservationIds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

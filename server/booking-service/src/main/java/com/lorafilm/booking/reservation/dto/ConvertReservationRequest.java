package com.lorafilm.booking.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ConvertReservationRequest {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotEmpty(message = "reservationIds cannot be empty")
    private List<Long> reservationIds;

    public ConvertReservationRequest() {
    }

    public ConvertReservationRequest(Long bookingId, List<Long> reservationIds) {
        this.bookingId = bookingId;
        this.reservationIds = reservationIds;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public List<Long> getReservationIds() {
        return reservationIds;
    }

    public void setReservationIds(List<Long> reservationIds) {
        this.reservationIds = reservationIds;
    }
}

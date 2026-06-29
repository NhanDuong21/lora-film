package com.project.bookingservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateBookingRequest {

    @NotEmpty(message = "reservationIds cannot be empty")
    private List<Long> reservationIds;

    public CreateBookingRequest() {
    }

    public CreateBookingRequest(List<Long> reservationIds) {
        this.reservationIds = reservationIds;
    }

    public List<Long> getReservationIds() {
        return reservationIds;
    }

    public void setReservationIds(List<Long> reservationIds) {
        this.reservationIds = reservationIds;
    }
}

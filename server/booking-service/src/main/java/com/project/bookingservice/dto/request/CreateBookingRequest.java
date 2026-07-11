package com.project.bookingservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateBookingRequest {

    @NotEmpty(message = "reservationIds cannot be empty")
    @org.hibernate.validator.constraints.UniqueElements(message = "Reservation IDs must be unique")
    private List<@jakarta.validation.constraints.NotNull(message = "Reservation ID cannot be null") @jakarta.validation.constraints.Positive(message = "Reservation ID must be positive") Long> reservationIds;

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

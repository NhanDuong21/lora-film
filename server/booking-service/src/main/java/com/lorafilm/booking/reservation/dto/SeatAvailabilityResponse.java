package com.lorafilm.booking.reservation.dto;

import java.util.List;

public class SeatAvailabilityResponse {

    private boolean available;
    private List<Long> unavailableSeats;

    public SeatAvailabilityResponse() {
    }

    public SeatAvailabilityResponse(boolean available, List<Long> unavailableSeats) {
        this.available = available;
        this.unavailableSeats = unavailableSeats;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public List<Long> getUnavailableSeats() {
        return unavailableSeats;
    }

    public void setUnavailableSeats(List<Long> unavailableSeats) {
        this.unavailableSeats = unavailableSeats;
    }
}

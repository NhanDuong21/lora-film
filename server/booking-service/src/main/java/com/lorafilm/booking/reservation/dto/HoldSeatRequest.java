package com.lorafilm.booking.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class HoldSeatRequest {

    @NotNull(message = "showtimeId is required")
    private Long showtimeId;

    @NotEmpty(message = "seatIds cannot be empty")
    private List<Long> seatIds;

    public HoldSeatRequest() {
    }

    public HoldSeatRequest(Long showtimeId, List<Long> seatIds) {
        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }
}

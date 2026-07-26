package com.lorafilm.movie.showtime.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BookingContextRequest {
    
    @NotEmpty(message = "Seat IDs cannot be empty")
    private List<Long> seatIds;

    public BookingContextRequest() {}

    public BookingContextRequest(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }
}

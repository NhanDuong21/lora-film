package com.lorafilm.movie.seat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;

import java.util.List;

public record BulkCreateSeatsRequest(
        @NotEmpty @Valid List<BulkSeatItemRequest> seats,
        @Min(1) Integer capacity
) {
    public BulkCreateSeatsRequest(List<BulkSeatItemRequest> seats) {
        this(seats, null);
    }
}

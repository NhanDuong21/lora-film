package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSeatStatusRequest(
        @NotNull SeatStatus status
) {}

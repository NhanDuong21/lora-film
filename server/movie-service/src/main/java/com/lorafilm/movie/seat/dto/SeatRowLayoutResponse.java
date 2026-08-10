package com.lorafilm.movie.seat.dto;

import java.util.List;

public record SeatRowLayoutResponse<T>(
        Integer positionRow,
        String rowLabel,
        List<T> seats
) {}

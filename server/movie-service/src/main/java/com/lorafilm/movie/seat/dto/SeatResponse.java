package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.seat.domain.enums.SeatStatus;

import java.time.Instant;

public record SeatResponse(
        String seatPublicId,
        String rowLabel,
        Integer seatNumber,
        String seatCode,
        Integer positionRow,
        Integer positionColumn,
        String pairGroup,
        SeatStatus status,
        SeatTypeResponse seatType,
        Instant createdAt,
        Instant updatedAt
) {}

package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.seat.domain.enums.SeatStatus;

import java.time.Instant;

public record AdminSeatItemResponse(
        String seatPublicId,
        String seatCode,
        String rowLabel,
        Integer seatNumber,
        Integer positionRow,
        Integer positionColumn,
        String pairGroup,
        SeatStatus status,
        SeatTypeResponse seatType,
        Instant createdAt,
        Instant updatedAt
) {}

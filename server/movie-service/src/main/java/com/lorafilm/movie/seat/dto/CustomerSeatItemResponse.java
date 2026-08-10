package com.lorafilm.movie.seat.dto;

public record CustomerSeatItemResponse(
        String seatPublicId,
        String seatCode,
        Integer seatNumber,
        Integer positionColumn,
        String pairGroup,
        CustomerSeatTypeResponse seatType,
        com.lorafilm.movie.seat.domain.enums.SeatStatus status,
        boolean selectable
) {}

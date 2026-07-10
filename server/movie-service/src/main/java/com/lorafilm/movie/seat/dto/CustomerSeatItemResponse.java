package com.lorafilm.movie.seat.dto;

public record CustomerSeatItemResponse(
        String seatPublicId,
        String seatCode,
        Integer seatNumber,
        Integer positionColumn,
        String pairGroup,
        CustomerSeatTypeResponse seatType
) {}

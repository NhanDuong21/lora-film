package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;

public record CustomerSeatTypeResponse(
        String publicId,
        SeatTypeCode code,
        String name
) {}

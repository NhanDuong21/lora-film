package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;

import java.time.Instant;

public record SeatTypeResponse(
        String publicId,
        SeatTypeCode code,
        String name,
        String description,
        ActiveStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

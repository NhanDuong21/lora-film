package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSeatTypeRequest(
        @NotNull SeatTypeCode code,
        @NotBlank @Size(max = 80) String name,
        @Size(max = 255) String description
) {}

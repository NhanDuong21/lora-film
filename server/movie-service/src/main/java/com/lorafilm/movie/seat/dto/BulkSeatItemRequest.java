package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BulkSeatItemRequest(
        @NotBlank String seatTypePublicId,
        @NotBlank @Size(max = 5) String rowLabel,
        @NotNull @Positive Integer seatNumber,
        @NotBlank @Size(max = 10) String seatCode,
        @NotNull @Positive Integer positionRow,
        @NotNull @Positive Integer positionColumn,
        @Size(max = 30) String pairGroup,
        @NotNull SeatStatus status
) {}

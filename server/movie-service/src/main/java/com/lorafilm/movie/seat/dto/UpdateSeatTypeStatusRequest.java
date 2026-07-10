package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSeatTypeStatusRequest(
        @NotNull ActiveStatus status
) {}

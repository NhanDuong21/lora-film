package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAuditoriumStatusRequest(
        @NotNull AuditoriumStatus status
) {}

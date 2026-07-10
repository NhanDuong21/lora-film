package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateAuditoriumRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull ScreenType screenType,
        @NotNull SoundType soundType,
        @NotNull @Positive Integer capacity,
        @NotNull @PositiveOrZero Integer cleaningBufferMinutes
) {}

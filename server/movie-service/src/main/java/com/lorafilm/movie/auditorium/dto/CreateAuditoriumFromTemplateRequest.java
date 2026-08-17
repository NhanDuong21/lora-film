package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateAuditoriumFromTemplateRequest(
        @NotBlank String cinemaPublicId,
        @NotBlank String templatePublicId,
        @NotBlank @Size(max = 80) String name,
        ScreenType screenType,
        SoundType soundType,
        @PositiveOrZero Integer cleaningBufferMinutes
) {}

package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;

import java.time.Instant;

public record AuditoriumResponse(
        String publicId,
        String cinemaPublicId,
        String cinemaName,
        String name,
        ScreenType screenType,
        SoundType soundType,
        Integer capacity,
        Integer cleaningBufferMinutes,
        AuditoriumStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;

import java.util.List;

public record AdminSeatLayoutResponse(
        String auditoriumPublicId,
        String auditoriumName,
        Integer capacity,
        ScreenType screenType,
        SoundType soundType,
        Integer cleaningBufferMinutes,
        AuditoriumStatus auditoriumStatus,
        Integer totalSeats,
        Integer activeSeats,
        Integer maintenanceSeats,
        List<SeatRowLayoutResponse<AdminSeatItemResponse>> rows
) {}

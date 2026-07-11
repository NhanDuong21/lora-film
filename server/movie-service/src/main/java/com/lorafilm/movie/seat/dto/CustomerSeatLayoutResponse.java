package com.lorafilm.movie.seat.dto;

import com.lorafilm.movie.auditorium.domain.enums.ScreenType;

import java.util.List;

public record CustomerSeatLayoutResponse(
        String auditoriumPublicId,
        String auditoriumName,
        Integer capacity,
        ScreenType screenType,
        List<SeatRowLayoutResponse<CustomerSeatItemResponse>> rows
) {}

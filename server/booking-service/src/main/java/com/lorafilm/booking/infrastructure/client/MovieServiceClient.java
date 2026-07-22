package com.lorafilm.booking.infrastructure.client;

import com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse;

public interface MovieServiceClient {
    ShowtimeSeatLayoutResponse getShowtimeSeatLayout(Long showtimeId);

    ShowtimeSeatLayoutResponse getShowtimeSeatLayoutByPublicId(String showtimePublicId);
}

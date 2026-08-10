package com.lorafilm.booking.booking.client;

import com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse;
import java.util.List;

public interface ShowtimeClient {

    ShowtimeBookingContext getBookingContext(Long showtimeId, List<Long> seatIds);

    default ShowtimeBookingContext getBookingContextByPublicId(String showtimePublicId, List<String> seatPublicIds) {
        throw new UnsupportedOperationException("Public Showtime booking context is not configured");
    }

    default ShowtimePresentationContext getPresentationByPublicId(String showtimePublicId) {
        return null;
    }

    ShowtimeSeatLayoutResponse getSeatLayout(Long showtimeId);
}

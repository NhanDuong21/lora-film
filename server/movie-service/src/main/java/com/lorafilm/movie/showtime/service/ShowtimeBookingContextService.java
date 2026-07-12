package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.dto.request.BookingContextRequest;
import com.lorafilm.movie.showtime.dto.response.BookingContextResponse;

public interface ShowtimeBookingContextService {
    BookingContextResponse getBookingContext(Long showtimeId, BookingContextRequest request);
}

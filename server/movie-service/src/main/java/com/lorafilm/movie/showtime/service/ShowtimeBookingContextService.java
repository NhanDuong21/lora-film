package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.dto.request.BookingContextRequest;
import com.lorafilm.movie.showtime.dto.response.BookingContextResponse;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieDto;

public interface ShowtimeBookingContextService {
    BookingContextResponse getBookingContext(Long showtimeId, BookingContextRequest request);
    BookingContextResponse getBookingContextByPublicId(String showtimePublicId, java.util.List<String> seatPublicIds);
    ShowtimeMovieDto getPresentationByPublicId(String showtimePublicId);
}

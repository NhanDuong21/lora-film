package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeBlockedSeatsRequest;
import com.lorafilm.movie.showtime.dto.response.ShowtimeSeatControlResponse;

public interface ShowtimeSeatBlockingService {
    ShowtimeSeatControlResponse getSeatControl(String showtimePublicId);

    ShowtimeSeatControlResponse blockSeats(
            String showtimePublicId,
            UpdateShowtimeBlockedSeatsRequest request);

    ShowtimeSeatControlResponse releaseSeats(
            String showtimePublicId,
            UpdateShowtimeBlockedSeatsRequest request);
}

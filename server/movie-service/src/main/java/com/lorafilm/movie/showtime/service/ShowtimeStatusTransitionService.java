package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;

public interface ShowtimeStatusTransitionService {
    AdminShowtimeResponse transitionStatus(String showtimePublicId, UpdateShowtimeStatusRequest request);
}

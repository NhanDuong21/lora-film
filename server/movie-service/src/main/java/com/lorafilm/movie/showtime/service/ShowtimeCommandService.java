package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.dto.request.CreateShowtimeRequest;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;

public interface ShowtimeCommandService {
    AdminShowtimeResponse createShowtime(CreateShowtimeRequest request);
    AdminShowtimeResponse updateShowtime(String showtimePublicId, UpdateShowtimeRequest request);
    void deleteBatch(String batchId);
}

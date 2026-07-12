package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.domain.entity.Showtime;

import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.response.ShowtimeStatusHistoryResponse;

import java.time.Instant;
import java.util.List;

public interface ShowtimeStatusHistoryService {
    void recordInitialHistory(Showtime showtime, Long changedBy);
    void recordTransitionHistory(Showtime showtime, ShowtimeStatus previousStatus, ShowtimeStatus newStatus, String reason, Long changedBy, Instant changedAt);
    List<ShowtimeStatusHistoryResponse> getShowtimeStatusHistory(String showtimePublicId);
}

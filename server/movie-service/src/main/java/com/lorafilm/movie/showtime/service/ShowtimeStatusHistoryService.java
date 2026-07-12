package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.domain.entity.Showtime;

public interface ShowtimeStatusHistoryService {
    void recordInitialHistory(Showtime showtime, Long changedBy);
}

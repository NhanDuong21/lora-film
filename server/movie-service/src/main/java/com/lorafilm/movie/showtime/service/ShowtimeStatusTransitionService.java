package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.dto.response.BatchStatusActionSummary;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;

public interface ShowtimeStatusTransitionService {
    AdminShowtimeResponse transitionStatus(String showtimePublicId, UpdateShowtimeStatusRequest request);
    BatchStatusActionSummary previewBatchStatus(String batchId, ShowtimeStatus targetStatus);
    BatchStatusActionSummary transitionBatchStatus(String batchId, UpdateShowtimeStatusRequest request);
}

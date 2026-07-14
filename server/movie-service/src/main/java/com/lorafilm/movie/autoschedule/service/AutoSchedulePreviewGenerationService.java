package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;

public interface AutoSchedulePreviewGenerationService {
    ShowtimeSchedulePreviewResponse generatePreview(GenerateShowtimeSchedulePreviewRequest request, Long adminUserId);
}

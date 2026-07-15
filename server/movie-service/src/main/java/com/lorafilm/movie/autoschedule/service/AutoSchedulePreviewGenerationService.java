package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;

public interface AutoSchedulePreviewGenerationService {
    ShowtimeSchedulePreviewSummaryResponse generatePreview(GenerateShowtimeSchedulePreviewRequest request, Long adminUserId);
}

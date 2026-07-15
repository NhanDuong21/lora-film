package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;

public interface AutoScheduleGenerateRequestNormalizer {
    NormalizedGeneratePreviewRequest normalize(GenerateShowtimeSchedulePreviewRequest request);
}

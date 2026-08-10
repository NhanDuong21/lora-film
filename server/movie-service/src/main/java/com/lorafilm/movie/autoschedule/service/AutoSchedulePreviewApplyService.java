package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.dto.request.ApplyShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ApplyShowtimeSchedulePreviewResponse;

public interface AutoSchedulePreviewApplyService {
    ApplyShowtimeSchedulePreviewResponse applyPreview(String previewPublicId, ApplyShowtimeSchedulePreviewRequest request);
}

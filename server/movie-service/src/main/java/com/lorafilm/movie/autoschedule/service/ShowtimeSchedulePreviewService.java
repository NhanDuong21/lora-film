package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;

public interface ShowtimeSchedulePreviewService {

    ShowtimeSchedulePreviewResponse getPreview(String previewPublicId);

    ShowtimeSchedulePreviewResponse updateSelections(
            String previewPublicId,
            UpdatePreviewItemSelectionsRequest request
    );
}

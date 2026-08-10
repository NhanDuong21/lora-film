package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery;
import com.lorafilm.movie.autoschedule.dto.request.CancelShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewPageResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;

public interface ShowtimeSchedulePreviewService {

    ShowtimeSchedulePreviewPageResponse getPreview(String previewPublicId, ShowtimeSchedulePreviewItemQuery query);

    ShowtimeSchedulePreviewSummaryResponse updateSelections(
            String previewPublicId,
            UpdatePreviewItemSelectionsRequest request
    );

    ShowtimeSchedulePreviewSummaryResponse cancelPreview(
            String previewPublicId,
            CancelShowtimeSchedulePreviewRequest request
    );
}

package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreviewHistoryQuery;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreviewHistoryItemResponse;
import com.lorafilm.movie.common.dto.PageResponse;

public interface AutoSchedulePreviewHistoryService {

    PageResponse<AutoSchedulePreviewHistoryItemResponse> getHistory(AutoSchedulePreviewHistoryQuery query);
}

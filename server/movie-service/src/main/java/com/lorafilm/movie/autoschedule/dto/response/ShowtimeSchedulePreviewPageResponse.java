package com.lorafilm.movie.autoschedule.dto.response;

import com.lorafilm.movie.common.api.PageResponse;

public class ShowtimeSchedulePreviewPageResponse {

    private ShowtimeSchedulePreviewSummaryResponse preview;
    private PageResponse<ShowtimeSchedulePreviewItemResponse> items;

    public ShowtimeSchedulePreviewPageResponse() {
    }

    public ShowtimeSchedulePreviewPageResponse(ShowtimeSchedulePreviewSummaryResponse preview, PageResponse<ShowtimeSchedulePreviewItemResponse> items) {
        this.preview = preview;
        this.items = items;
    }

    public ShowtimeSchedulePreviewSummaryResponse getPreview() {
        return preview;
    }

    public void setPreview(ShowtimeSchedulePreviewSummaryResponse preview) {
        this.preview = preview;
    }

    public PageResponse<ShowtimeSchedulePreviewItemResponse> getItems() {
        return items;
    }

    public void setItems(PageResponse<ShowtimeSchedulePreviewItemResponse> items) {
        this.items = items;
    }
}

package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;

import java.time.Instant;
import java.util.List;

public interface AutoScheduleApplyRevalidationService {
    void validateAll(ShowtimeSchedulePreview preview, List<ShowtimeSchedulePreviewItem> selectedItems, Instant now);
}

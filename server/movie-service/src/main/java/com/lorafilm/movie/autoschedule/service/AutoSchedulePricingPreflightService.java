package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePricingPreflightResponse;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;

import java.util.List;

public interface AutoSchedulePricingPreflightService {

    Evaluation evaluate(List<ShowtimeSchedulePreviewItem> selectedItems);

    record Evaluation(
            AutoSchedulePricingPreflightResponse response,
            List<PriceResolutionResult> resolutions
    ) {
    }
}

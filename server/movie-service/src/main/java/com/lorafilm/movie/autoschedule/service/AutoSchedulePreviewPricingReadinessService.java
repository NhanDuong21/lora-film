package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePricingPreflightResponse;

public interface AutoSchedulePreviewPricingReadinessService {

    AutoSchedulePricingPreflightResponse check(String previewPublicId, Long expectedVersion);
}

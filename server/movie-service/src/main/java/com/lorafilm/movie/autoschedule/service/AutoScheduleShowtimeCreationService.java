package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;

import java.util.List;

public interface AutoScheduleShowtimeCreationService {
    List<Showtime> createAll(
            List<ShowtimeSchedulePreviewItem> items,
            Long actorId,
            String batchId,
            List<PriceResolutionResult> pricingResolutions);
}

package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;

public interface CandidateCountEstimator {
    int estimate(AutoScheduleGenerationContext context);
}

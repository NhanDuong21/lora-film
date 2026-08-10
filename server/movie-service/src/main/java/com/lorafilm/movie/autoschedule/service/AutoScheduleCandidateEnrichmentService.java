package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;

import java.util.List;

public interface AutoScheduleCandidateEnrichmentService {
    void enrich(List<ShowtimeCandidate> candidates, AutoScheduleGenerationContext context);
}

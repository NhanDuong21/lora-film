package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.CandidateValidationResult;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;

public interface ShowtimeCandidateValidationService {
    CandidateValidationResult validate(ShowtimeCandidate candidate, AutoScheduleGenerationContext context);
}

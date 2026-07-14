package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.CandidateGenerationContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;

import java.util.List;

public interface ShowtimeCandidateGenerator {
    List<ShowtimeCandidate> generate(CandidateGenerationContext context);
}

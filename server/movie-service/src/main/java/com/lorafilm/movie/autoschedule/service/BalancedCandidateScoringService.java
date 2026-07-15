package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.CandidateScoreResult;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;

public interface BalancedCandidateScoringService {
    CandidateScoreResult score(ShowtimeCandidate candidate, CandidateScoringContext context);
}

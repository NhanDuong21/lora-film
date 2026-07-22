package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;

import java.util.List;

/**
 * One replayable scoring-and-selection contract for an auto-schedule strategy version.
 */
public interface AutoScheduleGenerationStrategy {

    String getStrategyVersion();

    /**
     * Scores the complete validated candidate universe and resolves its initial selection.
     * Set-level strategies receive the immutable generation context without repository access.
     */
    void scoreAndResolveDefaultSelection(List<ShowtimeCandidate> candidates,
                                         CandidateScoringContext context);
}

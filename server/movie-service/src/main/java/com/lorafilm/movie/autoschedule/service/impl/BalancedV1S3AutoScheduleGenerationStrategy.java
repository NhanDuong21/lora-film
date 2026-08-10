package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import com.lorafilm.movie.autoschedule.service.BalancedCandidateScoringService;
import com.lorafilm.movie.autoschedule.service.CandidateSelectionResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BalancedV1S3AutoScheduleGenerationStrategy implements AutoScheduleGenerationStrategy {

    private final BalancedCandidateScoringService scoringService;
    private final CandidateSelectionResolver selectionResolver;

    public BalancedV1S3AutoScheduleGenerationStrategy(BalancedCandidateScoringService scoringService,
                                                      CandidateSelectionResolver selectionResolver) {
        this.scoringService = scoringService;
        this.selectionResolver = selectionResolver;
    }

    @Override
    public String getStrategyVersion() {
        return AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3;
    }

    @Override
    public void scoreAndResolveDefaultSelection(List<ShowtimeCandidate> candidates,
                                                CandidateScoringContext context) {
        for (ShowtimeCandidate candidate : candidates) {
            var scoreResult = scoringService.score(candidate, context);
            candidate.setScore(scoreResult.getScore());
            candidate.setScoreBreakdown(scoreResult.getScoreBreakdown());
        }
        selectionResolver.resolveDefaultSelection(candidates);
    }
}

package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.CandidateScoreResult;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.BalancedCandidateScoringService;
import com.lorafilm.movie.autoschedule.service.CandidateSelectionResolver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BalancedV1S3AutoScheduleGenerationStrategyTest {

    @Test
    void delegatesScoringAndSelectionWithoutChangingS3Behavior() {
        BalancedCandidateScoringService scorer = mock(BalancedCandidateScoringService.class);
        CandidateSelectionResolver selector = mock(CandidateSelectionResolver.class);
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        CandidateScoringContext context = mock(CandidateScoringContext.class);
        CandidateScoreResult expected = new CandidateScoreResult(
                new BigDecimal("80.000"), Map.of("base", new BigDecimal("50.000")));
        when(scorer.score(candidate, context)).thenReturn(expected);

        BalancedV1S3AutoScheduleGenerationStrategy strategy =
                new BalancedV1S3AutoScheduleGenerationStrategy(scorer, selector);

        assertEquals(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3,
                strategy.getStrategyVersion());
        strategy.scoreAndResolveDefaultSelection(List.of(candidate), context);

        assertSame(expected.getScore(), candidate.getScore());
        assertSame(expected.getScoreBreakdown(), candidate.getScoreBreakdown());
        verify(selector).resolveDefaultSelection(List.of(candidate));
    }
}

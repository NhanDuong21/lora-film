package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
import com.lorafilm.movie.movie.dto.MovieReadinessDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieReadinessEvaluatorTest {

    private final MovieReadinessEvaluator evaluator = new MovieReadinessEvaluator();

    @Test
    void returnsReadyWhenThereAreNoIssues() {
        MovieReadinessDto readiness = evaluator.evaluate(healthyFacts());

        assertEquals(MovieHealthStatus.READY, readiness.getHealthStatus());
        assertEquals("READY", readiness.getClassification());
        assertTrue(readiness.getBlockers().isEmpty());
        assertTrue(readiness.getWarnings().isEmpty());
    }

    @Test
    void returnsWarningWhileKeepingLegacyReadyClassification() {
        MovieHealthFacts facts = new MovieHealthFacts(
                true,
                true,
                true,
                "Short Film",
                LocalDate.of(2026, 1, 1),
                AgeRating.P,
                15);

        MovieReadinessDto readiness = evaluator.evaluate(facts);

        assertEquals(MovieHealthStatus.WARNING, readiness.getHealthStatus());
        assertEquals("READY", readiness.getClassification());
        assertTrue(readiness.getBlockers().isEmpty());
        assertEquals(1, readiness.getWarnings().size());
        assertEquals(MovieReadinessEvaluator.SUSPICIOUS_DURATION, readiness.getWarnings().getFirst().getCode());
        assertDoesNotThrow(() -> evaluator.validatePublishConditions(facts));
    }

    @Test
    void returnsBlockedWithLegacyIncompleteClassification() {
        MovieHealthFacts facts = new MovieHealthFacts(
                false,
                true,
                true,
                "Canonical Movie",
                LocalDate.of(2026, 1, 1),
                AgeRating.P,
                120);

        MovieReadinessDto readiness = evaluator.evaluate(facts);

        assertEquals(MovieHealthStatus.BLOCKED, readiness.getHealthStatus());
        assertEquals("INCOMPLETE", readiness.getClassification());
        assertEquals(1, readiness.getBlockers().size());
        assertEquals(MovieReadinessEvaluator.NO_GENRE, readiness.getBlockers().getFirst().getCode());
        assertThrows(BusinessException.class, () -> evaluator.validatePublishConditions(facts));
    }

    private MovieHealthFacts healthyFacts() {
        return new MovieHealthFacts(
                true,
                true,
                true,
                "Canonical Movie",
                LocalDate.of(2026, 1, 1),
                AgeRating.P,
                120);
    }
}

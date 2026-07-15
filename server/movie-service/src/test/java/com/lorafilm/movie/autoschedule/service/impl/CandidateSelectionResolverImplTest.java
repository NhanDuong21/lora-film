package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CandidateSelectionResolverImplTest {

    private CandidateSelectionResolverImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new CandidateSelectionResolverImpl();
    }

    @Test
    void resolveDefaultSelection_selectsNonOverlappingGreedily() {
        Auditorium aud1 = new Auditorium();
        aud1.setId(1L);
        aud1.setPublicId("aud-1");

        MovieVersion mv1 = new MovieVersion();
        mv1.setPublicId("mv-1");

        Instant t0 = Instant.now();

        // High score, valid
        ShowtimeCandidate c1 = createCandidate(aud1, mv1, t0, t0.plus(120, ChronoUnit.MINUTES), 100);
        
        // Lower score, overlaps with c1
        ShowtimeCandidate c2 = createCandidate(aud1, mv1, t0.plus(60, ChronoUnit.MINUTES), t0.plus(180, ChronoUnit.MINUTES), 80);
        
        // Lower score, no overlap with c1
        ShowtimeCandidate c3 = createCandidate(aud1, mv1, t0.plus(130, ChronoUnit.MINUTES), t0.plus(250, ChronoUnit.MINUTES), 60);

        List<ShowtimeCandidate> candidates = Arrays.asList(c3, c2, c1); // Mixed order

        resolver.resolveDefaultSelection(candidates);

        // Sorting check
        assertEquals(c1, candidates.get(0));
        assertEquals(1, candidates.get(0).getRankingPosition());
        assertEquals(2, candidates.get(1).getRankingPosition());
        assertEquals(3, candidates.get(2).getRankingPosition());

        // Selection check
        assertTrue(c1.isSelected()); // Selected due to highest score
        assertFalse(c2.isSelected()); // Overlaps with c1
        assertTrue(c3.isSelected()); // Does not overlap with c1
    }

    private ShowtimeCandidate createCandidate(Auditorium aud, MovieVersion mv, Instant start, Instant occEnd, double score) {
        ShowtimeCandidate c = new ShowtimeCandidate();
        c.setAuditorium(aud);
        c.setMovieVersion(mv);
        c.setStartTime(start);
        c.setEndTime(start.plus(90, ChronoUnit.MINUTES)); // Dummy
        c.setOccupancyEndTime(occEnd);
        c.setScore(BigDecimal.valueOf(score));
        c.setValidationStatus(PreviewItemValidationStatus.VALID);
        return c;
    }
}

package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.model.CandidateValidationResult;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class ShowtimeCandidateValidationServiceImplTest {

    @Test
    void validate_mapsCanonicalClosureErrorToPreviewRejection() {
        ShowtimeValidationService sharedValidator = mock(ShowtimeValidationService.class);
        doThrow(new BusinessException(ErrorCode.SHOWTIME_OVERLAPS_CINEMA_CLOSURE))
                .when(sharedValidator).validateScheduling(any());
        ShowtimeCandidateValidationServiceImpl service =
                new ShowtimeCandidateValidationServiceImpl(sharedValidator);

        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setMovie(new Movie());
        candidate.setMovieVersion(new MovieVersion());
        candidate.setCinema(new Cinema());
        candidate.setAuditorium(new Auditorium());
        candidate.setStartTime(Instant.parse("2026-07-22T10:00:00Z"));
        candidate.setEndTime(Instant.parse("2026-07-22T12:00:00Z"));
        candidate.setOccupancyEndTime(Instant.parse("2026-07-22T12:15:00Z"));

        CandidateValidationResult result = service.validate(candidate);

        assertFalse(result.isValid());
        assertEquals(ErrorCode.SHOWTIME_OVERLAPS_CINEMA_CLOSURE.name(), result.getRejectionCode());
    }
}

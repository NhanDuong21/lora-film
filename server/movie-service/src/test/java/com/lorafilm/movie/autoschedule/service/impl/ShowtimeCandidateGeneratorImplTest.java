package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.model.CandidateGenerationContext;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.CinemaOperatingWindowResolver;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeCandidateGeneratorImplTest {

    @Mock
    private CinemaOperatingWindowResolver windowResolver;

    @InjectMocks
    private ShowtimeCandidateGeneratorImpl generator;

    @BeforeEach
    void setUp() {
    }

    @Test
    void generate_createsCandidatesBasedOnGranularity() {
        Cinema cinema = new Cinema();
        Auditorium aud = new Auditorium();
        aud.setCleaningBufferMinutes(15);
        
        Movie movie = new Movie();
        movie.setDurationMinutes(90);
        MovieVersion mv = new MovieVersion();
        mv.setMovie(movie);

        NormalizedGeneratePreviewRequest req = new NormalizedGeneratePreviewRequest(
                "cinema-1", LocalDate.now(), LocalDate.now(), List.of(), List.of(),
                30, 60, "key"
        );

        CandidateGenerationContext ctx = new CandidateGenerationContext(req, cinema, List.of(aud), List.of(mv));

        Instant winStart = Instant.parse("2023-10-01T08:00:00Z");
        Instant winEnd = Instant.parse("2023-10-01T10:00:00Z"); // 2 hours window
        
        when(windowResolver.resolve(any(), any(), any())).thenReturn(List.of(new OperatingWindow(winStart, winEnd)));

        List<ShowtimeCandidate> candidates = generator.generate(ctx);

        // Granularity is 30 mins. Window is 2 hours (120 mins).
        // Slots: 08:00, 08:30, 09:00, 09:30, 10:00
        // So 5 candidates.
        assertEquals(5, candidates.size());
        
        ShowtimeCandidate first = candidates.get(0);
        assertEquals(winStart, first.getStartTime());
        assertEquals(winStart.plus(90, ChronoUnit.MINUTES), first.getEndTime());
        assertEquals(winStart.plus(105, ChronoUnit.MINUTES), first.getOccupancyEndTime());
        
        ShowtimeCandidate second = candidates.get(1);
        assertEquals(winStart.plus(30, ChronoUnit.MINUTES), second.getStartTime());
    }
}

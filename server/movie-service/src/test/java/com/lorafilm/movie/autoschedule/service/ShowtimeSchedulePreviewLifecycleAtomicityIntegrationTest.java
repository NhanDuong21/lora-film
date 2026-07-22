package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.impl.ShowtimeSchedulePreviewLifecycleService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.integration.tmdb.scheduler.TmdbPersonSyncScheduler;
import com.lorafilm.movie.integration.tmdb.scheduler.TmdbSyncScheduler;
import com.lorafilm.movie.cinema.scheduler.CinemaStatusScheduler;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ShowtimeSchedulePreviewLifecycleAtomicityIntegrationTest {

    @Autowired private ShowtimeSchedulePreviewLifecycleService lifecycleService;
    @Autowired private ShowtimeSchedulePreviewRepository previewRepository;
    @Autowired private ShowtimeSchedulePreviewItemRepository itemRepository;
    @Autowired private CinemaRepository cinemaRepository;
    @Autowired private AuditoriumRepository auditoriumRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MovieVersionRepository movieVersionRepository;
    @MockBean private TmdbSyncScheduler tmdbSyncScheduler;
    @MockBean private TmdbPersonSyncScheduler tmdbPersonSyncScheduler;
    @MockBean private CinemaStatusScheduler cinemaStatusScheduler;

    @Test
    void duplicateItemFailureRollsBackEveryInsertAndPreviewTransition() {
        String suffix = UUID.randomUUID().toString();
        LocalDate date = LocalDate.now().plusDays(1);
        Cinema cinema = cinema(suffix);
        Movie movie = movie(suffix, date);
        MovieVersion version = movieVersion(movie);
        Auditorium auditorium = auditorium(cinema, suffix);
        NormalizedGeneratePreviewRequest request = new NormalizedGeneratePreviewRequest(
                cinema.getPublicId(), date, date, List.of(version.getPublicId()),
                List.of(auditorium.getPublicId()), 30, 60, "atomic-" + suffix);
        ShowtimeSchedulePreview preview = lifecycleService.createGeneratingPreview(
                request, cinema, "a".repeat(64), 1L);

        Instant start = date.atTime(10, 0).toInstant(java.time.ZoneOffset.UTC);
        ShowtimeCandidate first = candidate(cinema, auditorium, movie, version, start, 1);
        ShowtimeCandidate duplicate = candidate(cinema, auditorium, movie, version, start, 2);

        assertThrows(RuntimeException.class, () ->
                lifecycleService.persistGeneratedItemsAndMarkPreviewed(
                        preview, List.of(first, duplicate)));

        ShowtimeSchedulePreview unchanged = previewRepository.findById(preview.getId()).orElseThrow();
        assertEquals(SchedulePreviewStatus.GENERATING, unchanged.getStatus());
        assertEquals(0, unchanged.getTotalCandidateCount());
        assertEquals(0, unchanged.getValidCandidateCount());
        assertEquals(0, unchanged.getRejectedCandidateCount());
        assertEquals(0, unchanged.getSelectedCandidateCount());
        assertTrue(itemRepository.findAllByPreviewIdOrderByRankingPositionAscIdAsc(
                preview.getId()).isEmpty());
    }

    private ShowtimeCandidate candidate(Cinema cinema,
                                        Auditorium auditorium,
                                        Movie movie,
                                        MovieVersion version,
                                        Instant start,
                                        int rank) {
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setCinema(cinema);
        candidate.setAuditorium(auditorium);
        candidate.setMovie(movie);
        candidate.setMovieVersion(version);
        candidate.setStartTime(start);
        candidate.setEndTime(start.plus(60, ChronoUnit.MINUTES));
        candidate.setOccupancyEndTime(start.plus(75, ChronoUnit.MINUTES));
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setScore(new BigDecimal("65.000"));
        candidate.setScoreBreakdown(Map.of("base", new BigDecimal("50.000")));
        candidate.setRankingPosition(rank);
        candidate.setSelected(false);
        return candidate;
    }

    private Cinema cinema(String suffix) {
        Cinema value = new Cinema();
        value.setPublicId(UUID.randomUUID().toString());
        value.setSlug("s2-atomic-cinema-" + suffix);
        value.setName("S2 Atomic Cinema");
        value.setCity("HCMC");
        value.setAddress("Atomic Street");
        value.setTimezone("UTC");
        value.setStatus(CinemaStatus.ACTIVE);
        return cinemaRepository.saveAndFlush(value);
    }

    private Auditorium auditorium(Cinema cinema, String suffix) {
        Auditorium value = new Auditorium();
        value.setPublicId(UUID.randomUUID().toString());
        value.setCinema(cinema);
        value.setName("S2 Atomic Room " + suffix);
        value.setCapacity(100);
        value.setCleaningBufferMinutes(15);
        value.setStatus(AuditoriumStatus.ACTIVE);
        return auditoriumRepository.saveAndFlush(value);
    }

    private Movie movie(String suffix, LocalDate date) {
        Movie value = new Movie();
        value.setPublicId(UUID.randomUUID().toString());
        value.setTitle("S2 Atomic Movie");
        value.setSlug("s2-atomic-movie-" + suffix);
        value.setDurationMinutes(60);
        value.setAgeRating(AgeRating.T13);
        value.setReleaseDate(date.minusDays(1));
        value.setStatus(MovieStatus.NOW_SHOWING);
        return movieRepository.saveAndFlush(value);
    }

    private MovieVersion movieVersion(Movie movie) {
        MovieVersion value = new MovieVersion();
        value.setPublicId(UUID.randomUUID().toString());
        value.setMovie(movie);
        value.setVersionName("2D");
        value.setFormat(MovieFormat.TWO_D);
        value.setAudioLanguage("en");
        value.setStatus(ActiveStatus.ACTIVE);
        return movieVersionRepository.saveAndFlush(value);
    }
}

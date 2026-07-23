package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.impl.ShowtimeSchedulePreviewLifecycleService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import com.lorafilm.movie.cinema.scheduler.CinemaStatusScheduler;
import com.lorafilm.movie.integration.tmdb.scheduler.TmdbPersonSyncScheduler;
import com.lorafilm.movie.integration.tmdb.scheduler.TmdbSyncScheduler;

import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.SQL=OFF",
        "eureka.client.enabled=false"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AutoScheduleGenerationQueryBudgetIntegrationTest {

    @Autowired private AutoSchedulePreviewGenerationService generationService;
    @Autowired private AutoScheduleGenerationContextLoader contextLoader;
    @Autowired private MovieRepository movieRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @SpyBean private CinemaRepository cinemaRepository;
    @SpyBean private AuditoriumRepository auditoriumRepository;
    @SpyBean private MovieVersionRepository movieVersionRepository;
    @SpyBean private ShowtimeSchedulePreviewRepository previewRepository;
    @SpyBean private CinemaOperatingHourRepository operatingHourRepository;
    @SpyBean private CinemaClosurePeriodRepository closureRepository;
    @SpyBean private AuditoriumMaintenanceWindowRepository maintenanceRepository;
    @SpyBean private ShowtimeRepository showtimeRepository;
    @MockBean private ShowtimeSchedulePreviewLifecycleService lifecycleService;
    @MockBean private TmdbSyncScheduler tmdbSyncScheduler;
    @MockBean private TmdbPersonSyncScheduler tmdbPersonSyncScheduler;
    @MockBean private CinemaStatusScheduler cinemaStatusScheduler;

    private Cinema cinema;
    private MovieVersion version;
    private List<Auditorium> auditoriums;
    private LocalDate scheduleDate;
    private final AtomicInteger persistedCandidateCount = new AtomicInteger();

    @BeforeEach
    void setUp() {
        scheduleDate = LocalDate.now().plusDays(1);
        String suffix = UUID.randomUUID().toString();

        cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setSlug("s2-query-cinema-" + suffix);
        cinema.setName("S2 Query Cinema " + suffix);
        cinema.setCity("HCMC");
        cinema.setAddress("Query Budget Street");
        cinema.setTimezone("UTC");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema = cinemaRepository.saveAndFlush(cinema);

        CinemaOperatingHour hour = new CinemaOperatingHour();
        hour.setCinema(cinema);
        hour.setDayOfWeek(scheduleDate.getDayOfWeek().getValue());
        hour.setOpenTime(LocalTime.MIDNIGHT);
        hour.setCloseTime(LocalTime.of(23, 48));
        hour.setIsClosed(false);
        operatingHourRepository.saveAndFlush(hour);

        Movie movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setTitle("S2 Query Movie");
        movie.setSlug("s2-query-movie-" + suffix);
        movie.setDurationMinutes(1);
        movie.setAgeRating(AgeRating.T13);
        movie.setReleaseDate(scheduleDate.minusDays(1));
        movie.setEndDate(scheduleDate.plusDays(1));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie = movieRepository.saveAndFlush(movie);

        version = new MovieVersion();
        version.setPublicId(UUID.randomUUID().toString());
        version.setMovie(movie);
        version.setVersionName("2D");
        version.setFormat(MovieFormat.TWO_D);
        version.setAudioLanguage("en");
        version.setStatus(ActiveStatus.ACTIVE);
        version = movieVersionRepository.saveAndFlush(version);

        auditoriums = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Auditorium auditorium = new Auditorium();
            auditorium.setPublicId(UUID.randomUUID().toString());
            auditorium.setCinema(cinema);
            auditorium.setName("S2 Query Room " + suffix + " " + i);
            auditorium.setCapacity(100);
            auditorium.setCleaningBufferMinutes(0);
            auditorium.setStatus(AuditoriumStatus.ACTIVE);
            auditoriums.add(auditoriumRepository.saveAndFlush(auditorium));
        }

        doAnswer(invocation -> {
            var normalized = invocation.getArgument(0,
                    com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest.class);
            Cinema loadedCinema = invocation.getArgument(1, Cinema.class);
            return ShowtimeSchedulePreview.createGenerating(
                    loadedCinema, normalized.getScheduleFrom(), normalized.getScheduleTo(),
                    normalized.getSlotGranularityMinutes(), normalized.getPreviewTtlMinutes(),
                    invocation.getArgument(2, String.class), normalized.getIdempotencyKey(),
                    invocation.getArgument(3, String.class),
                    invocation.getArgument(4, Long.class), Instant.now());
        }).when(lifecycleService).createGeneratingPreview(any(), any(), any(), any(), any());
        doAnswer(invocation -> {
            List<?> candidates = invocation.getArgument(1, List.class);
            persistedCandidateCount.set(candidates.size());
            return null;
        }).when(lifecycleService).persistGeneratedItemsAndMarkPreviewed(any(), anyList());
    }

    @Test
    void repositoryReadsStayBoundedFromNinetySixToNearLimitAndJdbcCountsAreMeasuredSeparately() {
        runMeasuredCase(1, 15, 96, "small");
        runMeasuredCase(4, 1, 5_712, "medium");
        runMeasuredCase(7, 1, 9_996, "near-limit");
    }

    @Test
    void dormantS4ContextAddsExactlyOneBoundedCoverageRead() {
        clearRepositoryInvocations();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        NormalizedGeneratePreviewRequest normalized = new NormalizedGeneratePreviewRequest(
                cinema.getPublicId(), scheduleDate, scheduleDate,
                List.of(version.getPublicId()), List.of(auditoriums.getFirst().getPublicId()),
                15, 60, "s4-context-" + UUID.randomUUID());

        long started = System.nanoTime();
        AutoScheduleGenerationContext context = contextLoader.load(
                normalized, cinema, List.of(auditoriums.getFirst()), List.of(version),
                AutoScheduleStrategyVersions.BALANCED_V1_S4);
        long wallNanos = System.nanoTime() - started;

        assertEquals(AutoScheduleStrategyVersions.BALANCED_V1_S4, context.getStrategyVersion());
        assertEquals(0, context.getExistingShowtimeCounts().size());
        verify(operatingHourRepository).findByCinemaId(cinema.getId());
        verify(closureRepository).findOverlappingClosures(eq(cinema.getId()), any(), any());
        verify(maintenanceRepository).findActiveOverlapsForAutoSchedule(
                anyList(), eq(ActionStatus.ACTIVE), any(), any());
        verify(showtimeRepository).findBlockingFactsForAutoSchedule(anyList(), any(), any());
        verify(showtimeRepository).findCoverageFactsForAutoSchedule(
                eq(cinema.getId()), eq(List.of(version.getMovie().getId())), anyList(), any(), any());
        verifyNoMoreInteractions(operatingHourRepository, closureRepository,
                maintenanceRepository, showtimeRepository);

        System.out.printf(Locale.ROOT,
                "S4_CONTEXT_QUERY_BENCHMARK contextRepositoryReads=5 totalGenerationReads=9 "
                        + "coverageFacts=%d preparedStatements=%d wallMs=%.3f db=H2 profile=test%n",
                context.getExistingShowtimeCounts().size(), statistics.getPrepareStatementCount(),
                wallNanos / 1_000_000.0);
    }

    private void runMeasuredCase(int auditoriumCount,
                                 int granularity,
                                 int expectedCandidates,
                                 String label) {
        clearRepositoryInvocations();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        persistedCandidateCount.set(-1);

        Runtime runtime = Runtime.getRuntime();
        long usedBefore = runtime.totalMemory() - runtime.freeMemory();
        long started = System.nanoTime();
        generationService.generatePreview(request(auditoriumCount, granularity), 1L);
        long wallNanos = System.nanoTime() - started;
        long usedAfter = runtime.totalMemory() - runtime.freeMemory();

        assertEquals(expectedCandidates, persistedCandidateCount.get());
        verify(cinemaRepository).findByPublicIdAndDeletedAtIsNull(cinema.getPublicId());
        verify(previewRepository).findByGenerateIdempotencyKey(any());
        verify(auditoriumRepository).findByPublicIdInAndDeletedAtIsNull(anyList());
        verify(movieVersionRepository).findByPublicIdInWithMovieAndDeletedAtIsNull(anyList());
        verify(operatingHourRepository).findByCinemaId(cinema.getId());
        verify(closureRepository).findOverlappingClosures(eq(cinema.getId()), any(), any());
        verify(maintenanceRepository).findActiveOverlapsForAutoSchedule(
                anyList(), eq(ActionStatus.ACTIVE), any(), any());
        verify(showtimeRepository).findBlockingFactsForAutoSchedule(anyList(), any(), any());
        verifyNoMoreInteractions(cinemaRepository, previewRepository, auditoriumRepository,
                movieVersionRepository, operatingHourRepository, closureRepository,
                maintenanceRepository, showtimeRepository);

        System.out.printf(Locale.ROOT,
                "S2_QUERY_BENCHMARK label=%s candidates=%d repositoryReads=8 contextReads=7 " +
                        "preparedStatements=%d wallMs=%.3f observedHeapDeltaBytes=%d jdk=%s os=%s db=H2 profile=test warmup=Spring-context-and-fixture-only%n",
                label, expectedCandidates, statistics.getPrepareStatementCount(),
                wallNanos / 1_000_000.0, usedAfter - usedBefore,
                System.getProperty("java.version"), System.getProperty("os.name"));
    }

    private GenerateShowtimeSchedulePreviewRequest request(int auditoriumCount, int granularity) {
        GenerateShowtimeSchedulePreviewRequest request = new GenerateShowtimeSchedulePreviewRequest();
        request.setCinemaPublicId(cinema.getPublicId());
        request.setScheduleFrom(scheduleDate);
        request.setScheduleTo(scheduleDate);
        request.setAuditoriumPublicIds(auditoriums.subList(0, auditoriumCount).stream()
                .map(Auditorium::getPublicId).toList());
        request.setMovieVersionPublicIds(List.of(version.getPublicId()));
        request.setSlotGranularityMinutes(granularity);
        request.setPreviewTtlMinutes(60);
        request.setIdempotencyKey(UUID.randomUUID().toString());
        return request;
    }

    private void clearRepositoryInvocations() {
        clearInvocations(cinemaRepository, previewRepository, auditoriumRepository,
                movieVersionRepository, operatingHourRepository, closureRepository,
                maintenanceRepository, showtimeRepository);
    }
}

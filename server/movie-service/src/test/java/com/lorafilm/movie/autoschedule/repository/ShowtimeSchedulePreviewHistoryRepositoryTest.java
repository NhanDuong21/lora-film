package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewTestFactory;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreviewHistoryQuery;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.context.annotation.Import(com.lorafilm.movie.common.config.AuditConfig.class)
@DataJpaTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.stat=OFF"
})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ShowtimeSchedulePreviewHistoryRepositoryTest {

    @Autowired
    private ShowtimeSchedulePreviewRepository repository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private ShowtimeSchedulePreviewItemRepository itemRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieVersionRepository movieVersionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Cinema cinemaA;
    private Cinema cinemaB;

    @BeforeEach
    void setUp() {
        cinemaA = cinema("LoraFilm Quận 1", false);
        cinemaB = cinema("LoraFilm Đã đóng", true);
    }

    @Test
    void history_appliesExactAndBoundaryFilters_andIncludesDeletedCinema() {
        ShowtimeSchedulePreview first = preview(
                cinemaA,
                SchedulePreviewStatus.PREVIEWED,
                "BALANCED_V1",
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 22)
        );
        ShowtimeSchedulePreview deletedCinemaPreview = preview(
                cinemaB,
                SchedulePreviewStatus.APPLIED,
                "BALANCED_V1_S3",
                LocalDate.of(2026, 7, 23),
                LocalDate.of(2026, 7, 25)
        );
        Instant firstCreated = Instant.parse("2026-07-20T00:00:00Z");
        Instant secondCreated = Instant.parse("2026-07-22T00:00:00Z");
        setCreatedAt(first, firstCreated);
        setCreatedAt(deletedCinemaPreview, secondCreated);
        entityManager.clear();

        AutoSchedulePreviewHistoryQuery overlap = new AutoSchedulePreviewHistoryQuery();
        overlap.setScheduleFrom(LocalDate.of(2026, 7, 22));
        overlap.setScheduleTo(LocalDate.of(2026, 7, 23));
        overlap.setCreatedFrom(firstCreated);
        overlap.setCreatedTo(secondCreated.plusSeconds(1));

        Page<ShowtimeSchedulePreviewHistoryRow> result = repository.findHistory(
                overlap,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "scheduleFrom"))
        );

        assertThat(result.getContent())
                .extracting(ShowtimeSchedulePreviewHistoryRow::previewPublicId)
                .containsExactly(first.getPublicId(), deletedCinemaPreview.getPublicId());
        assertThat(result.getContent().get(1).cinemaName()).isEqualTo("LoraFilm Đã đóng");

        AutoSchedulePreviewHistoryQuery exact = new AutoSchedulePreviewHistoryQuery();
        exact.setCinemaPublicId(cinemaB.getPublicId());
        exact.setStatus(SchedulePreviewStatus.APPLIED);
        exact.setStrategyVersion("BALANCED_V1_S3");
        exact.setCreatedFrom(secondCreated);
        exact.setCreatedTo(secondCreated.plusSeconds(1));

        assertThat(repository.findHistory(
                exact,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent()).singleElement()
                .extracting(ShowtimeSchedulePreviewHistoryRow::previewPublicId)
                .isEqualTo(deletedCinemaPreview.getPublicId());

        exact.setCreatedTo(secondCreated);
        assertThat(repository.findHistory(
                exact,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        )).isEmpty();
    }

    @Test
    void history_usesStableIdTieBreak_andExecutesOnlyContentAndCountQueries() {
        ShowtimeSchedulePreview olderId = preview(
                cinemaA,
                SchedulePreviewStatus.PREVIEWED,
                "BALANCED_V1_S2",
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 22)
        );
        ShowtimeSchedulePreview newerId = preview(
                cinemaA,
                SchedulePreviewStatus.PREVIEWED,
                "BALANCED_V1_S2",
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 22)
        );
        Instant sameCreatedAt = Instant.parse("2026-07-22T01:00:00Z");
        setCreatedAt(olderId, sameCreatedAt);
        setCreatedAt(newerId, sameCreatedAt);
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Page<ShowtimeSchedulePreviewHistoryRow> result = repository.findHistory(
                new AutoSchedulePreviewHistoryQuery(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(result.getContent())
                .extracting(ShowtimeSchedulePreviewHistoryRow::previewPublicId)
                .startsWith(newerId.getPublicId(), olderId.getPublicId());
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        assertThat(statistics.getCollectionFetchCount()).isZero();
    }

    @Test
    void history_queryBudget_staysAtTwoQueriesWithTwoHundredPreviewsAndFourThousandItems() {
        List<ShowtimeSchedulePreview> previews = new ArrayList<>(200);
        for (int index = 0; index < 200; index++) {
            previews.add(buildPreview(
                    index % 2 == 0 ? cinemaA : cinemaB,
                    index % 3 == 0 ? SchedulePreviewStatus.APPLIED : SchedulePreviewStatus.PREVIEWED,
                    index % 2 == 0 ? "BALANCED_V1_S2" : "BALANCED_V1_S3",
                    LocalDate.of(2026, 8, 1).plusDays(index % 7),
                    LocalDate.of(2026, 8, 1).plusDays(index % 7)
            ));
        }
        repository.saveAllAndFlush(previews);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        entityManager.clear();
        statistics.clear();
        repository.findHistory(
                new AutoSchedulePreviewHistoryQuery(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        assertThat(statistics.getCollectionFetchCount()).isZero();

        Auditorium auditorium = performanceAuditorium();
        Movie movie = performanceMovie();
        MovieVersion movieVersion = performanceMovieVersion(movie);
        List<ShowtimeSchedulePreviewItem> items = new ArrayList<>(4_000);
        Instant baseTime = Instant.parse("2026-08-01T00:00:00Z");
        for (ShowtimeSchedulePreview preview : previews) {
            for (int itemIndex = 0; itemIndex < 20; itemIndex++) {
                Instant start = baseTime.plus(itemIndex * 3L, ChronoUnit.HOURS);
                ShowtimeSchedulePreviewItem item = new ShowtimeSchedulePreviewItem();
                item.setPublicId(UUID.randomUUID().toString());
                item.setPreview(preview);
                item.setMovie(movie);
                item.setMovieVersion(movieVersion);
                item.setCinema(preview.getCinema());
                item.setAuditorium(auditorium);
                item.setStartTime(start);
                item.setEndTime(start.plus(90, ChronoUnit.MINUTES));
                item.setOccupancyEndTime(start.plus(105, ChronoUnit.MINUTES));
                item.setScore(BigDecimal.ONE);
                item.setRankingPosition(itemIndex + 1);
                item.setValidationStatus(PreviewItemValidationStatus.VALID);
                item.setSelected(false);
                item.setApplyStatus(PreviewItemApplyStatus.PENDING);
                items.add(item);
            }
        }
        itemRepository.saveAllAndFlush(items);
        entityManager.clear();

        statistics.clear();
        long started = System.nanoTime();
        Page<ShowtimeSchedulePreviewHistoryRow> result = repository.findHistory(
                new AutoSchedulePreviewHistoryQuery(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        long wallNanos = System.nanoTime() - started;

        assertThat(previews).hasSize(200);
        assertThat(items).hasSize(4_000);
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.getTotalElements()).isEqualTo(200);
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        assertThat(statistics.getCollectionFetchCount()).isZero();
        System.out.printf(Locale.ROOT,
                "HISTORY_QUERY_BUDGET previews=%d items=%d returned=%d queries=%d preparedStatements=%d collectionFetches=%d wallMs=%.3f%n",
                previews.size(), items.size(), result.getNumberOfElements(),
                statistics.getQueryExecutionCount(), statistics.getPrepareStatementCount(),
                statistics.getCollectionFetchCount(), wallNanos / 1_000_000.0);
    }

    private Cinema cinema(String name, boolean deleted) {
        Cinema cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setName(name);
        cinema.setSlug("cinema-" + UUID.randomUUID());
        cinema.setCity("Hồ Chí Minh");
        cinema.setAddress("123 Test");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(CinemaStatus.ACTIVE);
        if (deleted) {
            cinema.performSoftDelete(99L);
        }
        return cinemaRepository.saveAndFlush(cinema);
    }

    private ShowtimeSchedulePreview preview(
            Cinema cinema,
            SchedulePreviewStatus status,
            String strategyVersion,
            LocalDate scheduleFrom,
            LocalDate scheduleTo
    ) {
        return repository.saveAndFlush(buildPreview(
                cinema, status, strategyVersion, scheduleFrom, scheduleTo
        ));
    }

    private ShowtimeSchedulePreview buildPreview(
            Cinema cinema,
            SchedulePreviewStatus status,
            String strategyVersion,
            LocalDate scheduleFrom,
            LocalDate scheduleTo
    ) {
        ShowtimeSchedulePreview preview = ShowtimeSchedulePreviewTestFactory.createPreview();
        preview.setPublicId(UUID.randomUUID().toString());
        preview.setCinema(cinema);
        preview.setScheduleFrom(scheduleFrom);
        preview.setScheduleTo(scheduleTo);
        preview.setTimezoneSnapshot("Asia/Ho_Chi_Minh");
        preview.setStrategy(AutoScheduleStrategy.BALANCED);
        preview.setStrategyVersion(strategyVersion);
        preview.setApplyMode(SchedulePreviewApplyMode.ALL_OR_NOTHING);
        preview.setStatus(status);
        preview.setSlotGranularityMinutes(15);
        preview.setTotalCandidateCount(10);
        preview.setValidCandidateCount(8);
        preview.setRejectedCandidateCount(2);
        preview.setSelectedCandidateCount(status == SchedulePreviewStatus.APPLIED ? 4 : 3);
        preview.setGeneratedAt(Instant.parse("2026-07-20T00:00:00Z"));
        preview.setExpiresAt(Instant.parse("2026-07-23T00:00:00Z"));
        preview.setGeneratedBy(1L);
        preview.setGenerateIdempotencyKey(UUID.randomUUID().toString());
        preview.setRequestFingerprint("a".repeat(64));
        return preview;
    }

    private Auditorium performanceAuditorium() {
        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setCinema(cinemaA);
        auditorium.setName("History Query Room");
        auditorium.setCapacity(100);
        auditorium.setCleaningBufferMinutes(15);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        return auditoriumRepository.saveAndFlush(auditorium);
    }

    private Movie performanceMovie() {
        Movie movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setTitle("History Query Movie");
        movie.setSlug("history-query-movie-" + UUID.randomUUID());
        movie.setDurationMinutes(90);
        movie.setAgeRating(AgeRating.T13);
        movie.setReleaseDate(LocalDate.of(2026, 7, 1));
        movie.setEndDate(LocalDate.of(2026, 9, 1));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        return movieRepository.saveAndFlush(movie);
    }

    private MovieVersion performanceMovieVersion(Movie movie) {
        MovieVersion version = new MovieVersion();
        version.setPublicId(UUID.randomUUID().toString());
        version.setMovie(movie);
        version.setVersionName("2D");
        version.setFormat(MovieFormat.TWO_D);
        version.setAudioLanguage("vi");
        version.setStatus(ActiveStatus.ACTIVE);
        return movieVersionRepository.saveAndFlush(version);
    }

    private void setCreatedAt(ShowtimeSchedulePreview preview, Instant createdAt) {
        jdbcTemplate.update(
                "update showtime_schedule_previews set created_at = ? where public_id = ?",
                Timestamp.from(createdAt),
                preview.getPublicId()
        );
    }
}

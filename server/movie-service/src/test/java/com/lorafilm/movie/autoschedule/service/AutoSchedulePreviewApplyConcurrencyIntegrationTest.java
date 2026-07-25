package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.ApplyShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public class AutoSchedulePreviewApplyConcurrencyIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("movie_db_test")
            .withUsername("root")
            .withPassword("12345678");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private AutoSchedulePreviewApplyService applyService;

    @Autowired
    private ShowtimeSchedulePreviewRepository previewRepository;

    @Autowired
    private ShowtimeSchedulePreviewItemRepository itemRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;
    
    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private MovieVersionRepository movieVersionRepository;
    
    @Autowired
    private com.lorafilm.movie.showtime.repository.ShowtimeRepository showtimeRepository;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String previewPublicId;
    private Long initialVersion;

    @BeforeEach
    void setUp() {
        // Clean up
        jdbcTemplate.execute("DELETE FROM cinema_operating_hours");
        jdbcTemplate.execute("DELETE FROM showtime_status_history");
        itemRepository.deleteAllInBatch();
        previewRepository.deleteAllInBatch();
        jdbcTemplate.execute("DELETE FROM showtimes");
        auditoriumRepository.deleteAllInBatch();
        cinemaRepository.deleteAllInBatch();
        movieVersionRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();

        // Setup base entities
        Cinema cinema = new Cinema();
        cinema.setPublicId("CINEMA_APPLY_" + UUID.randomUUID().toString().substring(0, 8));
        cinema.setSlug("cinema-apply-" + System.currentTimeMillis());
        cinema.setName("Apply Cinema");
        cinema.setCity("Test City");
        cinema.setAddress("Test Address");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema = cinemaRepository.saveAndFlush(cinema);
        
        for (int i = 1; i <= 7; i++) {
            jdbcTemplate.update("INSERT INTO cinema_operating_hours (cinema_id, day_of_week, open_time, close_time, is_closed, created_at, updated_at) VALUES (?, ?, '00:00:00', '23:59:59', false, NOW(), NOW())", cinema.getId(), i);
        }

        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId("AUD_APPLY_" + UUID.randomUUID().toString().substring(0, 8));
        auditorium.setName("Apply Auditorium");
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setCinema(cinema);
        auditorium.setCapacity(100);
        auditorium.setCleaningBufferMinutes(15);
        auditorium = auditoriumRepository.saveAndFlush(auditorium);
        
        Movie movie = new Movie();
        movie.setPublicId("MOVIE_APPLY_" + UUID.randomUUID().toString().substring(0, 8));
        movie.setSlug("movie-apply-" + System.currentTimeMillis());
        movie.setTitle("Apply Movie");
        movie.setOriginalTitle("Original Title");
        movie.setSynopsis("Test Synopsis");
        movie.setAgeRating(com.lorafilm.movie.movie.domain.enums.AgeRating.T13);
        movie.setCountry("US");
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(120);
        movie.setReleaseDate(java.time.LocalDate.now().minusDays(1));
        movie.setEndDate(java.time.LocalDate.now().plusDays(60));
        movie = movieRepository.saveAndFlush(movie);
        
        MovieVersion version = new MovieVersion();
        version.setPublicId("VERSION_APPLY_" + UUID.randomUUID().toString().substring(0, 8));
        version.setMovie(movie);
        version.setStatus(com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE);
        version.setVersionName("2D");
        version.setAudioLanguage("vi");
        version.setSubtitleLanguage("en");
        version.setFormat(com.lorafilm.movie.movie.domain.enums.MovieFormat.TWO_D);
        version = movieVersionRepository.saveAndFlush(version);

        ShowtimeSchedulePreview preview = ShowtimeSchedulePreview.createGenerating(
                cinema, java.time.LocalDate.now(), java.time.LocalDate.now(), 30, 60,
                UUID.randomUUID().toString(), "fp", 1L, Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "publicId", "PREVIEW_APPLY_" + UUID.randomUUID().toString().substring(0, 8));
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "status", SchedulePreviewStatus.PREVIEWED);
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "applyMode", SchedulePreviewApplyMode.ALL_OR_NOTHING);
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "expiresAt", Instant.now().plus(1, ChronoUnit.HOURS));
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "totalCandidateCount", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "selectedCandidateCount", 1);
        
        preview = previewRepository.saveAndFlush(preview);
        previewPublicId = preview.getPublicId();
        initialVersion = preview.getVersion();
        
        com.lorafilm.movie.autoschedule.model.ShowtimeCandidate candidate = new com.lorafilm.movie.autoschedule.model.ShowtimeCandidate();
        candidate.setMovie(movie);
        candidate.setMovieVersion(version);
        candidate.setCinema(cinema);
        candidate.setAuditorium(auditorium);
        
        Instant baseTime = Instant.now().plus(1, ChronoUnit.DAYS);
        candidate.setStartTime(baseTime);
        candidate.setEndTime(baseTime.plus(120, ChronoUnit.MINUTES));
        candidate.setOccupancyEndTime(baseTime.plus(135, ChronoUnit.MINUTES));
        
        candidate.setScore(BigDecimal.TEN);
        candidate.setScoreBreakdown(java.util.Map.of("test", BigDecimal.TEN));
        candidate.setRankingPosition(1);
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setSelected(true);
        
        ShowtimeSchedulePreviewItem item = ShowtimeSchedulePreviewItem.createItem(preview, candidate);
        org.springframework.test.util.ReflectionTestUtils.setField(item, "publicId", "ITEM_APPLY_" + UUID.randomUUID().toString().substring(0, 8));
        
        itemRepository.saveAndFlush(item);

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
    }
    
    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM cinema_operating_hours");
        jdbcTemplate.execute("DELETE FROM showtime_status_history");
        itemRepository.deleteAllInBatch();
        previewRepository.deleteAllInBatch();
        jdbcTemplate.execute("DELETE FROM showtimes");
        auditoriumRepository.deleteAllInBatch();
        cinemaRepository.deleteAllInBatch();
        movieVersionRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
    }

    @Test
    void testConcurrentApply_SamePreview_SameIdempotencyKey() throws Exception {
        int threads = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        List<Callable<Object>> tasks = new ArrayList<>();
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(threads);
        String sharedKey = UUID.randomUUID().toString();

        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                barrier.await(); // ensure all threads start simultaneously
                ApplyShowtimeSchedulePreviewRequest request = new ApplyShowtimeSchedulePreviewRequest();
                request.setIdempotencyKey(sharedKey); // SAME key to simulate idempotency race
                request.setExpectedVersion(initialVersion);
                
                try {
                    return applyService.applyPreview(previewPublicId, request);
                } catch (BusinessException e) {
                    return e.getErrorCode().name();
                }
            });
        }

        List<Future<Object>> results = executorService.invokeAll(tasks);
        executorService.shutdown();

        int successCount = 0;
        int errorCount = 0;

        for (Future<Object> result : results) {
            Object res = result.get();
            if (res instanceof com.lorafilm.movie.autoschedule.dto.response.ApplyShowtimeSchedulePreviewResponse) {
                successCount++;
            } else if (res instanceof String) {
                errorCount++;
                String err = (String) res;
                assertThat(err).isIn(
                    "AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS", 
                    "AUTO_SCHEDULE_PREVIEW_ALREADY_APPLIED"
                );
            }
        }

        // With the same key, exactly 1 should execute DB operations, others should either get APPLIED replay or IN_PROGRESS error
        // But since we catch exceptions in tasks, those that fail will count as errors. 
        // Replay success also returns ApplyShowtimeSchedulePreviewResponse!
        // Wait, if thread A commits and thread B reads, thread B returns ApplyShowtimeSchedulePreviewResponse (idempotency replay)
        // If thread B reads while A is in progress, thread B throws AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS
        // So successCount can be > 1 if some threads replay successfully!
        assertThat(successCount + errorCount).isEqualTo(3);
        assertThat(successCount).isGreaterThanOrEqualTo(1);

        // Verify DB state
        transactionTemplate.execute(status -> {
            ShowtimeSchedulePreview preview = previewRepository.findByPublicId(previewPublicId).orElseThrow();
            assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.APPLIED);
            
            List<ShowtimeSchedulePreviewItem> items = itemRepository.findAllByPreviewIdOrderByRankingPositionAscIdAsc(preview.getId());
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getApplyStatus()).isEqualTo(com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus.CREATED);
            assertThat(items.get(0).getCreatedShowtime()).isNotNull();
            return null;
        });

        // Exactly 1 showtime should be created, no duplicates
        assertThat(showtimeRepository.count()).isEqualTo(1);
        Integer historyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtime_status_history", Integer.class);
        assertThat(historyCount).isEqualTo(1);
    }
    
    @Test
    void testAtomicRollback_OnConflict() {
        // Setup: Inject a conflicting showtime in the database right before apply
        transactionTemplate.execute(status -> {
            ShowtimeSchedulePreviewItem item = itemRepository.findAll().get(0);
            com.lorafilm.movie.showtime.domain.entity.Showtime conflict = new com.lorafilm.movie.showtime.domain.entity.Showtime();
            conflict.setAuditorium(item.getAuditorium());
            conflict.setCinema(item.getCinema());
            conflict.setMovie(item.getMovie());
            conflict.setMovieVersion(item.getMovieVersion());
            conflict.setStartTime(item.getStartTime().minus(5, ChronoUnit.MINUTES));
            conflict.setEndTime(item.getEndTime().minus(5, ChronoUnit.MINUTES));
            conflict.setServiceDate(item.getServiceDate());
            conflict.setPublicId("CONFLICT_123");
            conflict.setStatus(com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus.DRAFT);
            showtimeRepository.saveAndFlush(conflict);
            return null;
        });
        
        long showtimeCountBefore = showtimeRepository.count();
        
        ApplyShowtimeSchedulePreviewRequest request = new ApplyShowtimeSchedulePreviewRequest();
        request.setIdempotencyKey(UUID.randomUUID().toString());
        request.setExpectedVersion(initialVersion);
        
        try {
            applyService.applyPreview(previewPublicId, request);
            org.junit.jupiter.api.Assertions.fail("Should have thrown SHOWTIME_OVERLAP_CONFLICT");
        } catch (BusinessException e) {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SHOWTIME_OVERLAP_CONFLICT);
        }
        
        // Assert rollback
        long showtimeCountAfter = showtimeRepository.count();
        assertThat(showtimeCountAfter).isEqualTo(showtimeCountBefore); // No new showtime created
        
        transactionTemplate.execute(status -> {
            ShowtimeSchedulePreview preview = previewRepository.findByPublicId(previewPublicId).orElseThrow();
            assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.PREVIEWED);
            assertThat(preview.getAppliedAt()).isNull();
            assertThat(preview.getAppliedBy()).isNull();
            assertThat(preview.getApplyIdempotencyKey()).isNull();
            
            List<ShowtimeSchedulePreviewItem> items = itemRepository.findAllByPreviewIdOrderByRankingPositionAscIdAsc(preview.getId());
            assertThat(items.get(0).getApplyStatus()).isEqualTo(com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus.PENDING);
            assertThat(items.get(0).getCreatedShowtime()).isNull();
            return null;
        });
    }
}

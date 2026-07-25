package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public class AutoSchedulePreviewGenerationConcurrencyIntegrationTest {

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
    private AutoSchedulePreviewGenerationService generationService;

    @Autowired
    private ShowtimeSchedulePreviewRepository previewRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieVersionRepository movieVersionRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Cinema cinema;
    private Auditorium auditorium;
    private Movie movie;
    private MovieVersion movieVersion;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE showtime_schedule_preview_items");
        jdbcTemplate.execute("TRUNCATE TABLE showtime_schedule_previews");
        jdbcTemplate.execute("TRUNCATE TABLE auditoriums");
        jdbcTemplate.execute("TRUNCATE TABLE cinemas");
        jdbcTemplate.execute("TRUNCATE TABLE movies");
        jdbcTemplate.execute("TRUNCATE TABLE movie_versions");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setSlug("movie-c-" + System.currentTimeMillis());
        movie.setTitle("Concurrency Auto Schedule Movie");
        movie.setOriginalTitle("Original Title");
        movie.setSynopsis("Test Synopsis");
        movie.setAgeRating(com.lorafilm.movie.movie.domain.enums.AgeRating.T13);
        movie.setCountry("US");
        movie.setStatus(com.lorafilm.movie.movie.domain.enums.MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.now().minusDays(1));
        movie.setEndDate(LocalDate.now().plusDays(60));
        movie = movieRepository.save(movie);

        movieVersion = new MovieVersion();
        movieVersion.setPublicId(UUID.randomUUID().toString());
        movieVersion.setMovie(movie);
        movieVersion.setVersionName("2D");
        movieVersion.setAudioLanguage("vi");
        movieVersion.setSubtitleLanguage("en");
        movieVersion.setFormat(com.lorafilm.movie.movie.domain.enums.MovieFormat.TWO_D);
        movieVersion.setStatus(com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE);
        movieVersion = movieVersionRepository.save(movieVersion);

        cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setSlug("cinema-c-" + System.currentTimeMillis());
        cinema.setName("Concurrency Cinema");
        cinema.setCity("Test City");
        cinema.setAddress("Test Address");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE);
        cinema = cinemaRepository.save(cinema);

        jdbcTemplate.update("INSERT INTO cinema_operating_hours (cinema_id, day_of_week, open_time, close_time, is_closed, created_at, updated_at) VALUES (?, ?, '08:00:00', '23:00:00', false, NOW(), NOW())", cinema.getId(), LocalDate.now().getDayOfWeek().getValue());

        auditorium = new Auditorium();
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setCinema(cinema);
        auditorium.setName("Concurrency Auditorium");
        auditorium.setScreenType(com.lorafilm.movie.auditorium.domain.enums.ScreenType.STANDARD);
        auditorium.setSoundType(com.lorafilm.movie.auditorium.domain.enums.SoundType.DOLBY_ATMOS);
        auditorium.setCapacity(100);
        auditorium.setStatus(com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.ACTIVE);
        auditorium.setCleaningBufferMinutes(15);
        auditorium = auditoriumRepository.save(auditorium);
    }

    @Test
    void concurrentGenerateSameIdempotencyKey_shouldHandleIdempotency() throws InterruptedException {
        int threads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);

        String idempotencyKey = UUID.randomUUID().toString();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    GenerateShowtimeSchedulePreviewRequest request = new GenerateShowtimeSchedulePreviewRequest();
                    request.setCinemaPublicId(cinema.getPublicId());
                    request.setScheduleFrom(LocalDate.now());
                    request.setScheduleTo(LocalDate.now());
                    request.setAuditoriumPublicIds(List.of(auditorium.getPublicId()));
                    request.setMovieVersionPublicIds(List.of(movieVersion.getPublicId()));
                    request.setSlotGranularityMinutes(30);
                    request.setPreviewTtlMinutes(60);
                    request.setIdempotencyKey(idempotencyKey);

                    generationService.generatePreview(request, 1L);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Exception: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 1 preview created, and the others got the same returned response
        assertEquals(3, successCount.get());
        
        System.out.println("=== MYSQL CONCURRENCY EVIDENCE ===");
        Integer previewCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtime_schedule_previews WHERE generate_idempotency_key = ?", Integer.class, idempotencyKey);
        System.out.println("Preview row count for key: " + previewCount);
        
        Integer itemCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM showtime_schedule_preview_items WHERE preview_id = (SELECT id FROM showtime_schedule_previews WHERE generate_idempotency_key = ?)", 
            Integer.class, idempotencyKey);
        System.out.println("Item row count for key: " + itemCount);
        System.out.println("=== END MYSQL CONCURRENCY EVIDENCE ===");

        assertEquals(1, previewCount);
    }
}

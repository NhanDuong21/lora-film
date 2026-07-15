package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.dto.request.CreateShowtimeRequest;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class ShowtimeSchedulingConcurrencyIntegrationTest {

    @Autowired
    private ShowtimeCommandService showtimeCommandService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieVersionRepository movieVersionRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository operatingHourRepository;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    private Movie movie;
    private MovieVersion movieVersion;
    private Cinema cinema;
    private Auditorium auditorium;

    @Autowired
    private com.lorafilm.movie.showtime.repository.ShowtimeStatusHistoryRepository showtimeStatusHistoryRepository;

    @Autowired
    private com.lorafilm.movie.seat.repository.SeatRepository seatRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE seats");
        jdbcTemplate.execute("TRUNCATE TABLE showtime_status_history");
        jdbcTemplate.execute("TRUNCATE TABLE showtimes");
        jdbcTemplate.execute("TRUNCATE TABLE auditoriums");
        jdbcTemplate.execute("TRUNCATE TABLE cinemas");
        jdbcTemplate.execute("TRUNCATE TABLE movies");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setSlug("movie-" + System.currentTimeMillis());
        movie.setTitle("Concurrency Movie");
        movie.setOriginalTitle("Original Title");
        movie.setSynopsis("Test Synopsis");
        movie.setAgeRating(com.lorafilm.movie.movie.domain.enums.AgeRating.T13);
        movie.setCountry("US");
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(120);
        movie.setReleaseDate(java.time.LocalDate.now().minusDays(1));
        movie.setEndDate(java.time.LocalDate.now().plusDays(60));
        movie = movieRepository.save(movie);

        movieVersion = new MovieVersion();
        movieVersion.setPublicId(UUID.randomUUID().toString());
        movieVersion.setMovie(movie);
        movieVersion.setVersionName("2D");
        movieVersion.setAudioLanguage("vi");
        movieVersion.setSubtitleLanguage("en");
        movieVersion.setFormat(com.lorafilm.movie.movie.domain.enums.MovieFormat.TWO_D);
        movieVersion.setStatus(ActiveStatus.ACTIVE);
        movieVersion = movieVersionRepository.save(movieVersion);

        cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setSlug("cinema-" + System.currentTimeMillis());
        cinema.setName("Concurrency Cinema");
        cinema.setCity("Test City");
        cinema.setAddress("Test Address");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema = cinemaRepository.save(cinema);

        com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour operatingHour = new com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour();
        operatingHour.setCinema(cinema);
        operatingHour.setDayOfWeek(1); // Monday
        operatingHour.setOpenTime(java.time.LocalTime.of(8, 0));
        operatingHour.setCloseTime(java.time.LocalTime.of(23, 0));
        operatingHour.setIsClosed(false);
        operatingHourRepository.save(operatingHour);

        auditorium = new Auditorium();
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setCinema(cinema);
        auditorium.setName("Concurrency Auditorium");
        auditorium.setScreenType(com.lorafilm.movie.auditorium.domain.enums.ScreenType.STANDARD);
        auditorium.setSoundType(com.lorafilm.movie.auditorium.domain.enums.SoundType.DOLBY_ATMOS);
        auditorium.setCapacity(100);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setCleaningBufferMinutes(15);
        auditorium = auditoriumRepository.save(auditorium);
    }

    @Test
    void concurrentOverlappingCreate_shouldOnlyAllowOne() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await(); // wait until all threads are ready
                    CreateShowtimeRequest request = new CreateShowtimeRequest();
                    request.setMoviePublicId(movie.getPublicId());
                    request.setMovieVersionPublicId(movieVersion.getPublicId());
                    request.setCinemaPublicId(cinema.getPublicId());
                    request.setAuditoriumPublicId(auditorium.getPublicId());
                    request.setStartTime(Instant.parse("2026-08-03T05:00:00Z")); // Same exact time
                    showtimeCommandService.createShowtime(request);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode().name().contains("CONFLICT") || e.getErrorCode().name().contains("OVERLAP")) {
                        conflictCount.incrementAndGet();
                    } else {
                        System.err.println("UNEXPECTED BUSINESS EXCEPTION: " + e.getErrorCode());
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // start all threads
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get());
        assertEquals(1, conflictCount.get());
        assertEquals(1, showtimeRepository.count());
    }
}

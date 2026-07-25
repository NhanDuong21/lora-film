package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeStatusHistoryRepository;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ShowtimeStatusTransitionIntegrationTest {

    @Autowired
    private ShowtimeStatusTransitionService transitionService;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private ShowtimeStatusHistoryRepository historyRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieVersionRepository movieVersionRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @MockBean
    private ShowtimePricingService showtimePricingService;

    private Showtime first;
    private Showtime second;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE showtime_status_history");
        jdbcTemplate.execute("TRUNCATE TABLE showtimes");
        jdbcTemplate.execute("TRUNCATE TABLE movie_versions");
        jdbcTemplate.execute("TRUNCATE TABLE auditoriums");
        jdbcTemplate.execute("TRUNCATE TABLE cinemas");
        jdbcTemplate.execute("TRUNCATE TABLE movies");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        when(currentUserProvider.getCurrentUserId()).thenReturn(77L);

        Movie movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setTitle("Atomic batch movie");
        movie.setSlug("atomic-batch-movie");
        movie.setDurationMinutes(90);
        movie.setAgeRating(AgeRating.P);
        movie.setReleaseDate(LocalDate.of(2026, 1, 1));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie = movieRepository.save(movie);

        MovieVersion version = new MovieVersion();
        version.setPublicId(UUID.randomUUID().toString());
        version.setMovie(movie);
        version.setVersionName("2D");
        version.setFormat(MovieFormat.TWO_D);
        version.setAudioLanguage("vi");
        version.setStatus(ActiveStatus.ACTIVE);
        version = movieVersionRepository.save(version);

        Cinema cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setName("Atomic batch cinema");
        cinema.setSlug("atomic-batch-cinema");
        cinema.setCity("Ho Chi Minh City");
        cinema.setAddress("Test address");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema = cinemaRepository.save(cinema);

        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setCinema(cinema);
        auditorium.setName("Atomic room");
        auditorium.setScreenType(ScreenType.STANDARD);
        auditorium.setSoundType(SoundType.STANDARD);
        auditorium.setCapacity(100);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setCleaningBufferMinutes(15);
        auditorium = auditoriumRepository.save(auditorium);

        first = showtime("atomic-first", movie, version, cinema, auditorium, "2099-08-22T10:00:00Z");
        second = showtime("atomic-second", movie, version, cinema, auditorium, "2099-08-22T12:00:00Z");
        showtimeRepository.saveAllAndFlush(List.of(first, second));
    }

    @Test
    void batchTransitionRollsBackEveryAffectedShowtimeWhenApplyFails() {
        AtomicInteger validationCalls = new AtomicInteger();
        doAnswer(invocation -> {
            if (validationCalls.incrementAndGet() == 4) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Injected failure");
            }
            return null;
        }).when(showtimePricingService).validateCompleteness(any(Showtime.class));

        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);

        assertThrows(
                BusinessException.class,
                () -> transitionService.transitionBatchStatus("atomic-batch", request));

        assertEquals(
                ShowtimeStatus.DRAFT,
                showtimeRepository.findByPublicIdAndDeletedAtIsNull(first.getPublicId())
                        .orElseThrow()
                        .getStatus());
        assertEquals(
                ShowtimeStatus.DRAFT,
                showtimeRepository.findByPublicIdAndDeletedAtIsNull(second.getPublicId())
                        .orElseThrow()
                        .getStatus());
        assertEquals(0, historyRepository.count());
    }

    private Showtime showtime(String publicId,
                              Movie movie,
                              MovieVersion version,
                              Cinema cinema,
                              Auditorium auditorium,
                              String start) {
        Showtime showtime = new Showtime();
        showtime.setPublicId(publicId);
        showtime.setMovie(movie);
        showtime.setMovieVersion(version);
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(Instant.parse(start));
        showtime.setEndTime(Instant.parse(start).plusSeconds(5400));
        showtime.setServiceDate(java.time.LocalDate.ofInstant(
                Instant.parse(start), java.time.ZoneId.of(cinema.getTimezone())));
        showtime.setStatus(ShowtimeStatus.DRAFT);
        showtime.setSource(ShowtimeSource.AUTO);
        showtime.setBatchId("atomic-batch");
        return showtime;
    }
}

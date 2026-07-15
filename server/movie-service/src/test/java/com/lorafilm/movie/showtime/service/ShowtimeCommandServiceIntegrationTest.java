package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.CreateShowtimeRequest;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ShowtimeCommandServiceIntegrationTest {

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

    @Autowired
    private ShowtimeStatusHistoryRepository statusHistoryRepository;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    private Movie movie;
    private MovieVersion movieVersion;
    private Cinema cinema;
    private Auditorium auditorium;

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
        movie.setTitle("Test Movie");
        movie.setOriginalTitle("Original Title");
        movie.setSynopsis("Test Synopsis");
        movie.setAgeRating(com.lorafilm.movie.movie.domain.enums.AgeRating.T13);
        movie.setCountry("US");
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(120);
        movie.setReleaseDate(java.time.LocalDate.now().minusDays(1));
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
        cinema.setName("Test Cinema");
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
        auditorium.setName("Auditorium 1");
        auditorium.setScreenType(com.lorafilm.movie.auditorium.domain.enums.ScreenType.STANDARD);
        auditorium.setSoundType(com.lorafilm.movie.auditorium.domain.enums.SoundType.DOLBY_ATMOS);
        auditorium.setCapacity(100);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setCleaningBufferMinutes(15);
        auditorium = auditoriumRepository.save(auditorium);
    }

    @Test
    void createShowtime_success_createsShowtimeAndHistory() {
        CreateShowtimeRequest request = new CreateShowtimeRequest();
        request.setMoviePublicId(movie.getPublicId());
        request.setMovieVersionPublicId(movieVersion.getPublicId());
        request.setCinemaPublicId(cinema.getPublicId());
        request.setAuditoriumPublicId(auditorium.getPublicId());
        // Choose a Monday since we only mocked Monday in operating hours
        // Let's use a fixed instant for a known Monday, or just use current time if we can force it
        // Actually, validation checks against Instant. We should mock operating hours properly or use a Monday date.
        // Assuming 2026-08-03T05:00:00Z (Monday 12:00 PM ICT)
        request.setStartTime(Instant.parse("2026-08-03T05:00:00Z"));

        var response = showtimeCommandService.createShowtime(request);
        
        assertNotNull(response);
        assertNotNull(response.getShowtimePublicId());
        
        var savedShowtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(response.getShowtimePublicId()).orElse(null);
        assertNotNull(savedShowtime);
        assertEquals(ShowtimeStatus.DRAFT, savedShowtime.getStatus());
        
        var history = statusHistoryRepository.findByShowtimeId(savedShowtime.getId());
        assertEquals(1, history.size());
        assertEquals(ShowtimeStatus.DRAFT, history.get(0).getNewStatus());
        assertNull(history.get(0).getPreviousStatus());
    }
}

package com.lorafilm.movie.showtime.repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;


import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@org.springframework.context.annotation.Import(com.lorafilm.movie.common.config.AuditConfig.class)
@DataJpaTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration"})
@org.springframework.test.context.ActiveProfiles("test")

class ShowtimeRepositoryIntegrationTest {

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieVersionRepository movieVersionRepository;

    private Auditorium auditorium;
    private Movie movie;
    private MovieVersion movieVersion;
    private Cinema cinema;

    @BeforeEach
    void setUp() {
        cinema = new Cinema();
        cinema.setName("Test Cinema");
        cinema.setSlug("test-cinema");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE);
        cinema.setAddress("123 Street");
        cinema.setCity("HCM");
        cinema.setDistrict("Q1");
        cinema.setPublicId(java.util.UUID.randomUUID().toString());
        cinemaRepository.save(cinema);

        auditorium = new Auditorium();
        auditorium.setName("Screen 1");
        auditorium.setCinema(cinema);
        auditorium.setStatus(com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.ACTIVE);
        auditorium.setCapacity(100);
        auditorium.setPublicId(java.util.UUID.randomUUID().toString());
        auditorium.setScreenType(com.lorafilm.movie.auditorium.domain.enums.ScreenType.STANDARD);
        auditorium.setSoundType(com.lorafilm.movie.auditorium.domain.enums.SoundType.DOLBY_ATMOS);
        auditorium.setCleaningBufferMinutes(15);
        auditoriumRepository.save(auditorium);

        movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setSlug("test-movie");
        movie.setStatus(com.lorafilm.movie.movie.domain.enums.MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(120);
        movie.setAgeRating(com.lorafilm.movie.movie.domain.enums.AgeRating.T13);
        movie.setReleaseDate(java.time.LocalDate.now());
        movie.setEndDate(java.time.LocalDate.now().plusDays(30));
        movie.setCountry("USA");
        movie.setOriginalTitle("Test Movie Org");
        movie.setPublicId(java.util.UUID.randomUUID().toString());
        movieRepository.save(movie);

        movieVersion = new MovieVersion();
        movieVersion.setMovie(movie);
        movieVersion.setVersionName("2D");
        movieVersion.setStatus(com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE);
        movieVersion.setAudioLanguage("EN");
        movieVersion.setSubtitleLanguage("VI");
        movieVersion.setDubLanguage("VI");
        movieVersion.setFormat(com.lorafilm.movie.movie.domain.enums.MovieFormat.TWO_D);
        movieVersion.setPublicId(java.util.UUID.randomUUID().toString());
        movieVersionRepository.save(movieVersion);
    }

    private Showtime createShowtime(Instant start, Instant end) {
        Showtime st = new Showtime();
        st.setAuditorium(auditorium);
        st.setCinema(cinema);
        st.setMovie(movie);
        st.setMovieVersion(movieVersion);
        st.setStartTime(start);
        st.setEndTime(end);
        st.setServiceDate(java.time.LocalDate.ofInstant(start,
                java.time.ZoneId.of(cinema.getTimezone())));
        st.setStatus(ShowtimeStatus.DRAFT);
        st.setPublicId(java.util.UUID.randomUUID().toString());
        return showtimeRepository.save(st);
    }

    @Test
    void findPotentialOverlaps_shouldDetectOverlap_whenIntervalsIntersect() {
        Instant t10 = Instant.parse("2026-07-15T10:00:00Z");
        Instant t12 = Instant.parse("2026-07-15T12:00:00Z");
        createShowtime(t10, t12);

        Instant t11 = Instant.parse("2026-07-15T11:00:00Z");
        Instant t13 = Instant.parse("2026-07-15T13:00:00Z");
        List<Showtime> overlaps = showtimeRepository.findPotentialOverlaps(auditorium.getId(), t11, t13);
        
        assertFalse(overlaps.isEmpty(), "Should detect overlap");
    }

    @Test
    void findPotentialOverlaps_shouldNotDetectOverlap_whenAdjacent() {
        Instant t10 = Instant.parse("2026-07-15T10:00:00Z");
        Instant t12 = Instant.parse("2026-07-15T12:00:00Z");
        createShowtime(t10, t12);

        Instant t12_14 = Instant.parse("2026-07-15T14:00:00Z");
        List<Showtime> overlaps = showtimeRepository.findPotentialOverlaps(auditorium.getId(), t12, t12_14);
        
        assertTrue(overlaps.isEmpty(), "Should NOT detect overlap for adjacent boundaries");
    }

    @Test
    void findCustomerBookingOptions_shouldExcludeStartedAndBoundaryShowtimes() {
        Instant now = Instant.parse("2026-07-26T11:53:00Z");
        Showtime past = createShowtime(
                Instant.parse("2026-07-26T02:00:00Z"),
                Instant.parse("2026-07-26T04:00:00Z"));
        Showtime boundary = createShowtime(
                now,
                Instant.parse("2026-07-26T13:53:00Z"));
        Showtime future = createShowtime(
                Instant.parse("2026-07-26T12:30:00Z"),
                Instant.parse("2026-07-26T14:30:00Z"));
        past.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        boundary.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        future.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        showtimeRepository.saveAllAndFlush(List.of(past, boundary, future));

        List<Showtime> result = showtimeRepository.findCustomerBookingOptions(
                movie.getSlug(),
                java.time.LocalDate.of(2026, 7, 26),
                java.time.LocalDate.of(2026, 7, 26),
                now);

        assertEquals(List.of(future.getPublicId()),
                result.stream().map(Showtime::getPublicId).toList());
    }
}

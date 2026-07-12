import os

file_path = 'server/movie-service/src/test/java/com/lorafilm/movie/showtime/repository/ShowtimeRepositoryIntegrationTest.java'
os.makedirs(os.path.dirname(file_path), exist_ok=True)

content = '''package com.lorafilm.movie.showtime.repository;

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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
        cinemaRepository.save(cinema);

        auditorium = new Auditorium();
        auditorium.setName("Screen 1");
        auditorium.setCinema(cinema);
        auditorium.setStatus(com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.ACTIVE);
        auditoriumRepository.save(auditorium);

        movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setSlug("test-movie");
        movie.setStatus(com.lorafilm.movie.movie.domain.enums.MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(120);
        movieRepository.save(movie);

        movieVersion = new MovieVersion();
        movieVersion.setMovie(movie);
        movieVersion.setVersionName("2D");
        movieVersion.setStatus(com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE);
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
        st.setStatus(ShowtimeStatus.SCHEDULED);
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
}
'''

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Created ShowtimeRepositoryIntegrationTest.java')

package com.lorafilm.movie.showtime.repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.config.AuditConfig;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(AuditConfig.class)
class ShowtimeCoverageFactRepositoryTest {

    private static final List<ShowtimeStatus> INCLUDED = List.of(
            ShowtimeStatus.DRAFT,
            ShowtimeStatus.OPEN_FOR_BOOKING,
            ShowtimeStatus.CLOSED,
            ShowtimeStatus.FINISHED);

    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private CinemaRepository cinemaRepository;
    @Autowired private AuditoriumRepository auditoriumRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MovieVersionRepository movieVersionRepository;

    @Test
    void projectionIsCinemaWideButFiltersMovieStatusDeletionAndBounds() {
        Cinema target = cinema("target");
        Cinema otherCinema = cinema("other");
        Auditorium requestedRoom = auditorium(target, "requested");
        Auditorium nonRequestedRoom = auditorium(target, "non-requested");
        Auditorium otherRoom = auditorium(otherCinema, "other");
        Movie requestedMovie = movie("requested");
        Movie otherMovie = movie("other");
        MovieVersion requestedVersion = version(requestedMovie, "requested");
        MovieVersion otherVersion = version(otherMovie, "other");
        Instant from = Instant.parse("2026-07-23T13:00:00Z");
        Instant to = Instant.parse("2026-07-23T19:00:00Z");

        showtime(target, requestedRoom, requestedMovie, requestedVersion,
                from, ShowtimeStatus.DRAFT, false);
        showtime(target, nonRequestedRoom, requestedMovie, requestedVersion,
                from.plusSeconds(60), ShowtimeStatus.OPEN_FOR_BOOKING, false);
        showtime(target, nonRequestedRoom, requestedMovie, requestedVersion,
                from.plusSeconds(120), ShowtimeStatus.CLOSED, false);
        showtime(target, nonRequestedRoom, requestedMovie, requestedVersion,
                from.plusSeconds(180), ShowtimeStatus.FINISHED, false);
        showtime(target, requestedRoom, requestedMovie, requestedVersion,
                from.plusSeconds(240), ShowtimeStatus.CANCELLED, false);
        showtime(target, requestedRoom, requestedMovie, requestedVersion,
                from.plusSeconds(300), ShowtimeStatus.DRAFT, true);
        showtime(target, requestedRoom, otherMovie, otherVersion,
                from.plusSeconds(360), ShowtimeStatus.DRAFT, false);
        showtime(otherCinema, otherRoom, requestedMovie, requestedVersion,
                from.plusSeconds(420), ShowtimeStatus.DRAFT, false);
        showtime(target, requestedRoom, requestedMovie, requestedVersion,
                to, ShowtimeStatus.DRAFT, false);

        List<AutoScheduleExistingShowtimeFact> facts =
                showtimeRepository.findCoverageFactsForAutoSchedule(
                        target.getId(), List.of(requestedMovie.getId()), INCLUDED, from, to);

        assertEquals(4, facts.size());
        assertEquals(List.of(
                        from,
                        from.plusSeconds(60),
                        from.plusSeconds(120),
                        from.plusSeconds(180)),
                facts.stream().map(AutoScheduleExistingShowtimeFact::getStartTime).toList());
        facts.forEach(fact -> {
            assertEquals(requestedMovie.getId(), fact.getMovieId());
            assertEquals(requestedMovie.getPublicId(), fact.getMoviePublicId());
        });
    }

    private Cinema cinema(String label) {
        String suffix = UUID.randomUUID().toString();
        Cinema cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setName(label + " cinema");
        cinema.setSlug(label + "-" + suffix);
        cinema.setCity("HCMC");
        cinema.setAddress("Address");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(CinemaStatus.ACTIVE);
        return cinemaRepository.saveAndFlush(cinema);
    }

    private Auditorium auditorium(Cinema cinema, String label) {
        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setCinema(cinema);
        auditorium.setName(label + " " + UUID.randomUUID());
        auditorium.setCapacity(100);
        auditorium.setCleaningBufferMinutes(15);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        return auditoriumRepository.saveAndFlush(auditorium);
    }

    private Movie movie(String label) {
        String suffix = UUID.randomUUID().toString();
        Movie movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setTitle(label + " movie");
        movie.setSlug(label + "-" + suffix);
        movie.setDurationMinutes(90);
        movie.setAgeRating(AgeRating.T13);
        movie.setReleaseDate(LocalDate.of(2026, 1, 1));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        return movieRepository.saveAndFlush(movie);
    }

    private MovieVersion version(Movie movie, String label) {
        MovieVersion version = new MovieVersion();
        version.setPublicId(UUID.randomUUID().toString());
        version.setMovie(movie);
        version.setVersionName(label);
        version.setFormat(MovieFormat.TWO_D);
        version.setAudioLanguage("en");
        version.setStatus(ActiveStatus.ACTIVE);
        return movieVersionRepository.saveAndFlush(version);
    }

    private void showtime(Cinema cinema,
                          Auditorium auditorium,
                          Movie movie,
                          MovieVersion version,
                          Instant start,
                          ShowtimeStatus status,
                          boolean deleted) {
        Showtime showtime = new Showtime();
        showtime.setPublicId(UUID.randomUUID().toString());
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setMovie(movie);
        showtime.setMovieVersion(version);
        showtime.setStartTime(start);
        showtime.setEndTime(start.plusSeconds(5_400));
        showtime.setStatus(status);
        if (deleted) {
            showtime.setDeletedAt(Instant.parse("2026-07-22T00:00:00Z"));
        }
        showtimeRepository.saveAndFlush(showtime);
    }
}

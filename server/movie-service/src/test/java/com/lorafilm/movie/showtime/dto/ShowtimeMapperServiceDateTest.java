package com.lorafilm.movie.showtime.dto;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowtimeMapperServiceDateTest {

    @Test
    void keepsPersistedServiceDateWhileFormattingOvernightClocksInCinemaTimezone() {
        Cinema cinema = new Cinema();
        cinema.setPublicId("cinema");
        cinema.setTimezone("America/Los_Angeles");
        Movie movie = new Movie();
        movie.setPublicId("movie");
        MovieVersion version = new MovieVersion();
        version.setPublicId("version");
        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId("auditorium");

        Showtime showtime = new Showtime();
        showtime.setPublicId("showtime");
        showtime.setCinema(cinema);
        showtime.setMovie(movie);
        showtime.setMovieVersion(version);
        showtime.setAuditorium(auditorium);
        showtime.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        showtime.setServiceDate(LocalDate.of(2026, 7, 24));
        showtime.setStartTime(Instant.parse("2026-07-25T06:30:00Z"));
        showtime.setEndTime(Instant.parse("2026-07-25T09:00:00Z"));

        ShowtimeDto result = new ShowtimeMapper().toDto(showtime);

        assertEquals(LocalDate.of(2026, 7, 24), result.getServiceDate());
        assertEquals(LocalDateTime.of(2026, 7, 24, 23, 30), result.getLocalStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 25, 2, 0), result.getLocalEndTime());
    }
}

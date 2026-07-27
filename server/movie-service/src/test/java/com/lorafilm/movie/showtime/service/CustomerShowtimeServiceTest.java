package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class CustomerShowtimeServiceTest {

    @Test
    void bookingOptionsUsePersistedServiceDateAndCinemaLocalClocks() {
        ShowtimeRepository repository = mock(ShowtimeRepository.class);
        CustomerShowtimeService service = new CustomerShowtimeService(
                repository, mock(ShowtimePriceRepository.class),
                mock(ShowtimeBlockedSeatRepository.class), mock(SeatService.class));
        LocalDate persistedDate = LocalDate.of(2026, 7, 24);

        Cinema cinema = new Cinema();
        cinema.setPublicId("cinema-public");
        cinema.setSlug("cinema");
        cinema.setName("Cinema");
        cinema.setTimezone("America/Los_Angeles");
        Movie movie = new Movie();
        movie.setPublicId("movie-public");
        movie.setSlug("movie");
        MovieVersion version = new MovieVersion();
        version.setPublicId("version-public");
        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId("auditorium-public");

        Showtime showtime = new Showtime();
        showtime.setPublicId("showtime-public");
        showtime.setMovie(movie);
        showtime.setMovieVersion(version);
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        showtime.setServiceDate(persistedDate);
        showtime.setStartTime(Instant.parse("2026-07-25T06:30:00Z"));
        showtime.setEndTime(Instant.parse("2026-07-25T09:00:00Z"));
        when(repository.findCustomerBookingOptions(
                eq("movie"), eq(persistedDate), eq(persistedDate), any(Instant.class)))
                .thenReturn(List.of(showtime, showtime));

        var result = service.getBookingOptions("movie", persistedDate, persistedDate);

        assertEquals(1, result.size());
        assertEquals(persistedDate, result.getFirst().serviceDate());
        assertEquals(LocalDateTime.of(2026, 7, 24, 23, 30), result.getFirst().localStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 25, 2, 0), result.getFirst().localEndTime());
        verify(repository).findCustomerBookingOptions(
                eq("movie"), eq(persistedDate), eq(persistedDate), any(Instant.class));
    }
}

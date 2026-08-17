package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.dto.CreateCinemaClosurePeriodRequest;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.integration.BookingSeatAvailabilityClient;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CinemaClosureImpactServiceImplTest {

    @Test
    void aggregatesShowtimeAndBookingImpactFromBackendFacts() {
        CinemaRepository cinemaRepository = mock(CinemaRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        BookingSeatAvailabilityClient bookingClient = mock(BookingSeatAvailabilityClient.class);
        CinemaClosureImpactServiceImpl service = new CinemaClosureImpactServiceImpl(
                cinemaRepository, showtimeRepository, seatRepository, bookingClient);

        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-1");
        cinema.setName("LoraFilm Test");
        Auditorium auditorium = new Auditorium();
        auditorium.setId(2L);
        auditorium.setName("Phòng 01");
        auditorium.setCinema(cinema);
        Movie movie = new Movie();
        movie.setTitle("Movie");

        Showtime showtime = new Showtime();
        showtime.setId(3L);
        showtime.setPublicId("showtime-1");
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setMovie(movie);
        showtime.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        showtime.setStartTime(Instant.parse("2026-08-20T10:00:00Z"));
        showtime.setEndTime(Instant.parse("2026-08-20T12:00:00Z"));

        Seat first = new Seat();
        first.setId(10L);
        Seat second = new Seat();
        second.setId(11L);
        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-1"))
                .thenReturn(Optional.of(cinema));
        when(showtimeRepository.findCinemaPotentialOverlaps(
                1L,
                Instant.parse("2026-08-20T09:00:00Z"),
                Instant.parse("2026-08-20T13:00:00Z")))
                .thenReturn(List.of(showtime));
        when(seatRepository.findAdminLayoutByAuditoriumId(2L))
                .thenReturn(List.of(first, second));
        when(bookingClient.check(3L, List.of(10L, 11L)))
                .thenReturn(new BookingSeatAvailabilityClient.AvailabilityResult(
                        true, List.of(11L)));

        CreateCinemaClosurePeriodRequest request = new CreateCinemaClosurePeriodRequest();
        request.setStartTime(Instant.parse("2026-08-20T09:00:00Z"));
        request.setEndTime(Instant.parse("2026-08-20T13:00:00Z"));

        var impact = service.preview("cinema-1", request);

        assertThat(impact.affectedShowtimeCount()).isEqualTo(1);
        assertThat(impact.openForBookingCount()).isEqualTo(1);
        assertThat(impact.occupiedSeatCount()).isEqualTo(1);
        assertThat(impact.bookingDataComplete()).isTrue();
        assertThat(impact.showtimes()).singleElement()
                .extracting("auditoriumName")
                .isEqualTo("Phòng 01");
    }
}

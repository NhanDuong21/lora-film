package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

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
                mock(ShowtimeBlockedSeatRepository.class), mock(SeatService.class),
                Clock.systemUTC());
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

    @Test
    void coupleLayoutAllocatesConfiguredPairPriceAcrossTwoPhysicalSeats() {
        ShowtimeRepository repository = mock(ShowtimeRepository.class);
        ShowtimePriceRepository priceRepository = mock(ShowtimePriceRepository.class);
        ShowtimeBlockedSeatRepository blockedSeatRepository =
                mock(ShowtimeBlockedSeatRepository.class);
        SeatService seatService = mock(SeatService.class);
        CustomerShowtimeService service = new CustomerShowtimeService(
                repository, priceRepository, blockedSeatRepository, seatService,
                Clock.systemUTC());

        Cinema cinema = new Cinema();
        cinema.setPublicId("cinema-public");
        cinema.setName("Cinema");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        Movie movie = new Movie();
        movie.setPublicId("movie-public");
        movie.setSlug("movie");
        movie.setTitle("Movie");
        MovieVersion version = new MovieVersion();
        version.setPublicId("version-public");
        Auditorium auditorium = new Auditorium();
        auditorium.setId(9L);
        auditorium.setPublicId("auditorium-public");

        Showtime showtime = new Showtime();
        showtime.setId(77L);
        showtime.setPublicId("showtime-public");
        showtime.setMovie(movie);
        showtime.setMovieVersion(version);
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        showtime.setServiceDate(LocalDate.now());
        showtime.setStartTime(Instant.now().plusSeconds(3600));
        showtime.setEndTime(Instant.now().plusSeconds(7200));

        SeatType coupleType = new SeatType();
        coupleType.setId(3L);
        coupleType.setCode(SeatTypeCode.COUPLE);
        coupleType.setName("Ghế đôi");
        Seat first = coupleSeat(91L, "seat-i1", "I1", 1, coupleType);
        Seat second = coupleSeat(92L, "seat-i2", "I2", 2, coupleType);

        ShowtimePrice price = new ShowtimePrice();
        price.setSeatType(coupleType);
        price.setPrice(new BigDecimal("156000"));
        price.setCurrency("VND");

        when(repository.findByPublicIdAndDeletedAtIsNull("showtime-public"))
                .thenReturn(Optional.of(showtime));
        when(priceRepository.findByShowtimeId(77L)).thenReturn(List.of(price));
        when(blockedSeatRepository.findByShowtimeIdAndStatus(77L, ActionStatus.ACTIVE))
                .thenReturn(List.of());
        when(seatService.getSeatsByAuditoriumId(9L)).thenReturn(List.of(first, second));

        var layout = service.getSeatLayout("showtime-public");

        assertEquals(2, layout.seats().size());
        assertEquals(new BigDecimal("78000"), layout.seats().get(0).price());
        assertEquals(new BigDecimal("78000"), layout.seats().get(1).price());
        assertEquals(
                new BigDecimal("156000"),
                layout.seats().stream()
                        .map(seat -> seat.price())
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Seat coupleSeat(
            Long id, String publicId, String code, int column, SeatType coupleType) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setPublicId(publicId);
        seat.setSeatCode(code);
        seat.setRowLabel("I");
        seat.setSeatNumber(column);
        seat.setPositionRow(9);
        seat.setPositionColumn(column);
        seat.setPairGroup("I-01");
        seat.setSeatType(coupleType);
        seat.setStatus(SeatStatus.ACTIVE);
        return seat;
    }
}

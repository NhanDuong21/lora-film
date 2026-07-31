package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.SeatLayoutDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMapper;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShowtimeSeatLayoutQueryTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimePriceRepository showtimePriceRepository;

    @Mock
    private ShowtimeBlockedSeatRepository showtimeBlockedSeatRepository;

    @Mock
    private SeatService seatService;

    @Mock
    private ShowtimeMapper showtimeMapper;

    @Mock
    private MovieMediaRepository movieMediaRepository;

    @InjectMocks
    private ShowtimeQueryServiceImpl showtimeQueryService;

    private Showtime showtime;
    private Seat seat;
    private ShowtimePrice showtimePrice;
    private SeatType seatType;

    @BeforeEach
    void setUp() {
        Movie movie = new Movie();
        movie.setPublicId("movie-123");

        MovieVersion movieVersion = new MovieVersion();
        movieVersion.setPublicId("version-123");

        Cinema cinema = new Cinema();
        cinema.setPublicId("cinema-123");

        Auditorium auditorium = new Auditorium();
        auditorium.setId(10L);
        auditorium.setPublicId("auditorium-123");

        showtime = new Showtime();
        showtime.setId(100L);
        showtime.setPublicId("showtime-id");
        showtime.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        showtime.setMovie(movie);
        showtime.setMovieVersion(movieVersion);
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(java.time.Instant.now().plusSeconds(3600));

        seatType = new SeatType();
        seatType.setId(5L);
        seatType.setCode(SeatTypeCode.VIP);

        seat = new Seat();
        seat.setId(1L);
        seat.setPublicId("seat-123");
        seat.setSeatCode("A1");
        seat.setSeatType(seatType);
        seat.setStatus(SeatStatus.ACTIVE);

        showtimePrice = new ShowtimePrice();
        showtimePrice.setId(50L);
        showtimePrice.setShowtime(showtime);
        showtimePrice.setSeatType(seatType);
        showtimePrice.setPrice(new BigDecimal("150000")); // The price saved in snapshot
        showtimePrice.setCurrency("VND");
    }

    @Test
    void testGetSeatLayoutReturnsSnapshotPrice() {
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("showtime-id")).thenReturn(Optional.of(showtime));
        when(seatService.getSeatsByAuditoriumId(10L)).thenReturn(Collections.singletonList(seat));
        when(showtimePriceRepository.findByShowtimeId(100L)).thenReturn(Collections.singletonList(showtimePrice));
        when(showtimeBlockedSeatRepository.findByShowtimeIdAndStatus(100L, ActionStatus.ACTIVE)).thenReturn(Collections.emptyList());

        SeatLayoutDto layout = showtimeQueryService.getSeatLayout("showtime-id");

        assertEquals(1, layout.getSeats().size());
        SeatLayoutDto.SeatPriceDto returnedSeat = layout.getSeats().get(0);

        // Core business requirements verification
        assertEquals("A1", returnedSeat.getSeatCode());
        assertEquals(SeatTypeCode.VIP.name(), returnedSeat.getSeatType());
        
        // Assert it uses the price from ShowtimePrice (150000), not dynamically fetched.
        assertEquals(new BigDecimal("150000"), returnedSeat.getPrice());
        
        // Assert currency defaults to VND correctly
        assertEquals("VND", returnedSeat.getCurrency());
        
        // Ensure it's not blocked since we returned empty block list
        assertFalse(returnedSeat.isBlockedForShowtime());
    }

    @Test
    void testGetSeatLayoutExposesCouplePairMetadata() {
        seatType.setCode(SeatTypeCode.COUPLE);
        seat.setSeatCode("I1");
        seat.setRowLabel("I");
        seat.setPositionRow(9);
        seat.setPositionColumn(1);
        seat.setPairGroup("I-01");

        Seat pairedSeat = new Seat();
        pairedSeat.setId(2L);
        pairedSeat.setPublicId("seat-124");
        pairedSeat.setSeatCode("I2");
        pairedSeat.setRowLabel("I");
        pairedSeat.setPositionRow(9);
        pairedSeat.setPositionColumn(2);
        pairedSeat.setPairGroup("I-01");
        pairedSeat.setSeatType(seatType);
        pairedSeat.setStatus(SeatStatus.ACTIVE);

        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("showtime-id"))
                .thenReturn(Optional.of(showtime));
        when(seatService.getSeatsByAuditoriumId(10L)).thenReturn(List.of(seat, pairedSeat));
        when(showtimePriceRepository.findByShowtimeId(100L))
                .thenReturn(Collections.singletonList(showtimePrice));
        when(showtimeBlockedSeatRepository.findByShowtimeIdAndStatus(100L, ActionStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        SeatLayoutDto layout = showtimeQueryService.getSeatLayout("showtime-id");

        assertEquals(2, layout.getSeats().size());
        assertEquals("I-01", layout.getSeats().get(0).getPairGroup());
        assertEquals(2L, layout.getSeats().get(0).getPairedSeatId());
        assertEquals(1L, layout.getSeats().get(1).getPairedSeatId());
        assertEquals(new BigDecimal("75000"), layout.getSeats().get(0).getPrice());
        assertEquals(new BigDecimal("75000"), layout.getSeats().get(1).getPrice());
    }
}

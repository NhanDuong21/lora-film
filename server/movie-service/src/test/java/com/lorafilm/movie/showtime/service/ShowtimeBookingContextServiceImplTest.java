package com.lorafilm.movie.showtime.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.ShowtimeMapper;
import com.lorafilm.movie.showtime.dto.request.BookingContextRequest;
import com.lorafilm.movie.showtime.dto.response.BookingContextResponse;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimePriceRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;

@ExtendWith(MockitoExtension.class)
class ShowtimeBookingContextServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimePriceRepository showtimePriceRepository;

    @Mock
    private ShowtimeBlockedSeatRepository showtimeBlockedSeatRepository;

    @Mock
    private SeatRepository seatRepository;

    private SeatService seatService;
    private ShowtimeMapper showtimeMapper;

    @InjectMocks
    private ShowtimeBookingContextServiceImpl showtimeService;

    private Showtime showtime;
    private Auditorium auditorium;
    private Seat seat1;
    private Seat seat2;
    private SeatType seatTypeStandard;

    @BeforeEach
    void setUp() {
        seatService = new com.lorafilm.movie.seat.service.impl.SeatServiceImpl(seatRepository, null, null);
        showtimeMapper = new ShowtimeMapper();
        showtimeService = new ShowtimeBookingContextServiceImpl(showtimeRepository, showtimePriceRepository, showtimeBlockedSeatRepository, seatService, showtimeMapper);

        Movie movie = new Movie();
        movie.setId(1L);
        movie.setPublicId("movie-1");
        movie.setSlug("movie-1");
        movie.setTitle("Movie 1");

        MovieVersion movieVersion = new MovieVersion();
        movieVersion.setId(1L);
        movieVersion.setPublicId("mv-1");

        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-1");
        cinema.setTimezone("Asia/Ho_Chi_Minh");

        auditorium = new Auditorium();
        auditorium.setId(1L);
        auditorium.setPublicId("aud-1");

        showtime = new Showtime();
        showtime.setId(10L);
        showtime.setMovie(movie);
        showtime.setMovieVersion(movieVersion);
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        showtime.setStartTime(java.time.Instant.now());
        showtime.setEndTime(java.time.Instant.now().plusSeconds(7200));

        seatTypeStandard = new SeatType();
        seatTypeStandard.setId(1L);
        seatTypeStandard.setCode(SeatTypeCode.STANDARD);

        seat1 = new Seat();
        seat1.setId(101L);
        seat1.setAuditorium(auditorium);
        seat1.setSeatType(seatTypeStandard);
        seat1.setStatus(SeatStatus.ACTIVE);
        seat1.setSeatCode("A1");

        seat2 = new Seat();
        seat2.setId(102L);
        seat2.setAuditorium(auditorium);
        seat2.setSeatType(seatTypeStandard);
        seat2.setStatus(SeatStatus.ACTIVE);
        seat2.setSeatCode("A2");
    }

    // removed query tests

    @Test
    void getBookingContext_valid_returnsContext() {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Arrays.asList(101L, 102L));

        when(showtimeRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByIdInAndDeletedAtIsNull(request.getSeatIds())).thenReturn(Arrays.asList(seat1, seat2));

        ShowtimePrice price = new ShowtimePrice();
        price.setSeatType(seatTypeStandard);
        price.setPrice(new BigDecimal("100000"));
        price.setCurrency("VND");

        when(showtimePriceRepository.findByShowtimeId(10L)).thenReturn(Collections.singletonList(price));

        BookingContextResponse response = showtimeService.getBookingContext(10L, request);

        assertNotNull(response);
        assertNotNull(response.getShowtime());
        assertEquals(10L, response.getShowtime().getId());
        assertEquals(1L, response.getMovieId());
        assertEquals(1L, response.getCinemaId());
        assertEquals(1L, response.getAuditoriumId());
        assertEquals(2, response.getSelectedSeats().size());
        assertNotNull(response.getPricing());
        assertEquals(new BigDecimal("200000"), response.getPricing().getTotalAmount());
        assertEquals("VND", response.getPricing().getCurrency());
        assertNotNull(response.getBookingExpiredAt());
    }

    @Test
    void getBookingContext_showtimeNotFound_throwsException() {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Collections.singletonList(101L));

        when(showtimeRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> showtimeService.getBookingContext(10L, request));
    }

    @Test
    void getBookingContext_showtimeNotOpen_throwsException() {
        showtime.setStatus(ShowtimeStatus.DRAFT);
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Collections.singletonList(101L));

        when(showtimeRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(showtime));

        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeService.getBookingContext(10L, request));
        assertEquals(ErrorCode.INVALID_SHOWTIME_STATUS_TRANSITION, ex.getErrorCode());
    }

    @Test
    void getBookingContext_duplicateSeatIds_throwsException() {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Arrays.asList(101L, 101L));

        when(showtimeRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(showtime));

        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeService.getBookingContext(10L, request));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Duplicate seat IDs"));
    }

    @Test
    void getBookingContext_seatNotFound_throwsException() {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Arrays.asList(101L, 999L));

        when(showtimeRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByIdInAndDeletedAtIsNull(request.getSeatIds())).thenReturn(Collections.singletonList(seat1));

        assertThrows(ResourceNotFoundException.class, () -> showtimeService.getBookingContext(10L, request));
    }

    @Test
    void getBookingContext_seatBelongsToAnotherAuditorium_throwsException() {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Collections.singletonList(101L));

        Auditorium otherAuditorium = new Auditorium();
        otherAuditorium.setId(2L);
        seat1.setAuditorium(otherAuditorium);

        when(showtimeRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByIdInAndDeletedAtIsNull(request.getSeatIds())).thenReturn(Collections.singletonList(seat1));

        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeService.getBookingContext(10L, request));
        assertEquals(ErrorCode.SEAT_BELONGS_TO_ANOTHER_AUDITORIUM, ex.getErrorCode());
    }

    @Test
    void getBookingContext_seatInactive_throwsException() {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Collections.singletonList(101L));

        seat1.setStatus(SeatStatus.MAINTENANCE);

        when(showtimeRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByIdInAndDeletedAtIsNull(request.getSeatIds())).thenReturn(Collections.singletonList(seat1));

        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeService.getBookingContext(10L, request));
        assertEquals(ErrorCode.SEAT_INACTIVE, ex.getErrorCode());
    }

    @Test
    void getBookingContext_missingPrice_throwsException() {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Collections.singletonList(101L));

        when(showtimeRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByIdInAndDeletedAtIsNull(request.getSeatIds())).thenReturn(Collections.singletonList(seat1));
        when(showtimePriceRepository.findByShowtimeId(10L)).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeService.getBookingContext(10L, request));
        assertEquals(ErrorCode.SHOWTIME_PRICE_MISSING, ex.getErrorCode());
    }
    @Test
    void getBookingContext_seatBlocked_throwsException() {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Collections.singletonList(101L));

        when(showtimeRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByIdInAndDeletedAtIsNull(request.getSeatIds())).thenReturn(Collections.singletonList(seat1));
        
        com.lorafilm.movie.showtime.domain.entity.ShowtimeBlockedSeat blockedSeat = new com.lorafilm.movie.showtime.domain.entity.ShowtimeBlockedSeat();
        blockedSeat.setSeat(seat1);
        when(showtimeBlockedSeatRepository.findByShowtimeIdAndStatus(10L, com.lorafilm.movie.common.enums.ActionStatus.ACTIVE))
            .thenReturn(Collections.singletonList(blockedSeat));

        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeService.getBookingContext(10L, request));
        assertEquals(ErrorCode.SEAT_BLOCKED_FOR_SHOWTIME, ex.getErrorCode());
    }
}

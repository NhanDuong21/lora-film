package com.lorafilm.movie.showtime.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.SeatLayoutDto;
import com.lorafilm.movie.showtime.dto.ShowtimeDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMapper;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;

@ExtendWith(MockitoExtension.class)
class ShowtimeQueryServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimePriceRepository showtimePriceRepository;

    @Mock
    private ShowtimeBlockedSeatRepository showtimeBlockedSeatRepository;

    @Mock
    private SeatRepository seatRepository;

    private SeatService seatService;
    @Mock
    private ShowtimeMapper showtimeMapper;

    @Mock
    private MovieMediaRepository movieMediaRepository;

    @InjectMocks
    private ShowtimeQueryServiceImpl showtimeService;

    private Showtime showtime;
    private Auditorium auditorium;
    private Seat seat1;
    private Seat seat2;
    private SeatType seatTypeStandard;

    @BeforeEach
    void setUp() {
        seatService = new com.lorafilm.movie.seat.service.impl.SeatServiceImpl(seatRepository, null, null);
        showtimeService = new ShowtimeQueryServiceImpl(showtimeRepository, showtimePriceRepository,
                showtimeBlockedSeatRepository, seatService, showtimeMapper, movieMediaRepository);

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
        java.time.Instant futureStart = java.time.Instant.now().plusSeconds(3600);
        showtime.setStartTime(futureStart);
        showtime.setEndTime(futureStart.plusSeconds(7200));

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

    @Test
    void getShowtimeByPublicId_validOpenForBooking_returnsDto() {
        showtime.setPublicId("public-123");
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123")).thenReturn(Optional.of(showtime));
        ShowtimeDto dto = new ShowtimeDto();
        when(showtimeMapper.toDto(showtime)).thenReturn(dto);

        ShowtimeDto result = showtimeService.getShowtimeByPublicId("public-123");
        assertNotNull(result);
    }

    @Test
    void getShowtimeByPublicId_statusDraft_throwsException() {
        showtime.setPublicId("public-123");
        showtime.setStatus(ShowtimeStatus.DRAFT);
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123")).thenReturn(Optional.of(showtime));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> showtimeService.getShowtimeByPublicId("public-123"));
        assertEquals("Showtime not found or not open for booking", ex.getMessage());
    }

    @Test
    void getShowtimeByPublicId_alreadyStarted_throwsException() {
        showtime.setPublicId("public-123");
        showtime.setStartTime(java.time.Instant.now().minusSeconds(1));
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123"))
                .thenReturn(Optional.of(showtime));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> showtimeService.getShowtimeByPublicId("public-123"));
        assertEquals("Showtime not found or not open for booking", ex.getMessage());
    }

    @Test
    void getShowtimeByPublicId_statusClosed_throwsException() {
        showtime.setPublicId("public-123");
        showtime.setStatus(ShowtimeStatus.CLOSED);
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123")).thenReturn(Optional.of(showtime));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> showtimeService.getShowtimeByPublicId("public-123"));
        assertEquals("Showtime not found or not open for booking", ex.getMessage());
    }

    @Test
    void getShowtimeByPublicId_statusCancelled_throwsException() {
        showtime.setPublicId("public-123");
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123")).thenReturn(Optional.of(showtime));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> showtimeService.getShowtimeByPublicId("public-123"));
        assertEquals("Showtime not found or not open for booking", ex.getMessage());
    }

    @Test
    void getShowtimeByPublicId_statusFinished_throwsException() {
        showtime.setPublicId("public-123");
        showtime.setStatus(ShowtimeStatus.FINISHED);
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123")).thenReturn(Optional.of(showtime));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> showtimeService.getShowtimeByPublicId("public-123"));
        assertEquals("Showtime not found or not open for booking", ex.getMessage());
    }

    @Test
    void getShowtimeByPublicId_notFound_throwsException() {
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> showtimeService.getShowtimeByPublicId("public-123"));
        assertEquals("Showtime not found", ex.getMessage());
    }

    @Test
    void getSeatLayout_missingSnapshotFailsInsteadOfReturningZero() {
        showtime.setPublicId("public-123");
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123")).thenReturn(Optional.of(showtime));

        when(seatRepository.findByAuditoriumIdAndDeletedAtIsNull(auditorium.getId()))
                .thenReturn(Arrays.asList(seat1, seat2));
        when(showtimePriceRepository.findByShowtimeId(10L)).thenReturn(Collections.emptyList());
        when(showtimeBlockedSeatRepository.findByShowtimeIdAndStatus(10L,
                com.lorafilm.movie.common.enums.ActionStatus.ACTIVE)).thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> showtimeService.getSeatLayout("public-123"));
        assertEquals(ErrorCode.PRICING_INCOMPLETE, exception.getErrorCode());
    }

    @Test
    void getSeatLayout_accessibleSeat_usesStandardPriceAndRemainsBookable() {
        showtime.setPublicId("public-123");
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123"))
                .thenReturn(Optional.of(showtime));

        SeatType disabledSeatType = new SeatType();
        disabledSeatType.setId(4L);
        disabledSeatType.setCode(SeatTypeCode.DISABLED);

        Seat disabledSeat = new Seat();
        disabledSeat.setId(104L);
        disabledSeat.setAuditorium(auditorium);
        disabledSeat.setSeatType(disabledSeatType);
        disabledSeat.setStatus(SeatStatus.ACTIVE);
        disabledSeat.setSeatCode("A4");

        when(seatRepository.findByAuditoriumIdAndDeletedAtIsNull(auditorium.getId()))
                .thenReturn(Collections.singletonList(disabledSeat));
        ShowtimePrice standardPrice = new ShowtimePrice();
        standardPrice.setSeatType(seatTypeStandard);
        standardPrice.setPrice(new BigDecimal("75000"));
        standardPrice.setCurrency("VND");
        when(showtimePriceRepository.findByShowtimeId(10L)).thenReturn(List.of(standardPrice));
        when(showtimeBlockedSeatRepository.findByShowtimeIdAndStatus(10L,
                com.lorafilm.movie.common.enums.ActionStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        SeatLayoutDto result = showtimeService.getSeatLayout("public-123");

        assertEquals(1, result.getSeats().size());
        SeatLayoutDto.SeatPriceDto seat = result.getSeats().get(0);
        assertEquals("DISABLED", seat.getSeatType());
        assertEquals(new BigDecimal("75000"), seat.getPrice());
        assertEquals("VND", seat.getCurrency());
        assertFalse(seat.isBlockedForShowtime());
    }

    @Test
    void getSeatLayout_statusDraft_throwsException() {
        showtime.setPublicId("public-123");
        showtime.setStatus(ShowtimeStatus.DRAFT);
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123")).thenReturn(Optional.of(showtime));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> showtimeService.getSeatLayout("public-123"));
        assertEquals("Showtime not found or not open for booking", ex.getMessage());
    }

    @Test
    void getSeatLayout_notFound_throwsException() {
        when(showtimeRepository.findByPublicIdAndDeletedAtIsNull("public-123")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> showtimeService.getSeatLayout("public-123"));
        assertEquals("Showtime not found", ex.getMessage());
    }

}

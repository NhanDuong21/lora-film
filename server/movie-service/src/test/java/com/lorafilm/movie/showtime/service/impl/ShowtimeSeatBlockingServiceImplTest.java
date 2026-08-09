package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.entity.ShowtimeBlockedSeat;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeBlockedSeatsRequest;
import com.lorafilm.movie.showtime.integration.BookingSeatAvailabilityClient;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeSeatBlockingServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private ShowtimeBlockedSeatRepository blockedSeatRepository;
    @Mock
    private BookingSeatAvailabilityClient bookingAvailabilityClient;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private ShowtimeSeatBlockingServiceImpl service;
    private Showtime showtime;
    private List<Seat> coupleSeats;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-08T08:00:00Z"), ZoneOffset.UTC);
        service = new ShowtimeSeatBlockingServiceImpl(
                showtimeRepository,
                seatRepository,
                blockedSeatRepository,
                bookingAvailabilityClient,
                currentUserProvider,
                clock);

        Cinema cinema = new Cinema();
        cinema.setPublicId("cinema-1");
        cinema.setName("LoraFilm Landmark 81");
        cinema.setTimezone("Asia/Ho_Chi_Minh");

        Auditorium auditorium = new Auditorium();
        auditorium.setId(20L);
        auditorium.setPublicId("room-1");
        auditorium.setName("Screen 01 - Standard");

        Movie movie = new Movie();
        movie.setTitle("Chỉ Một Đêm");

        showtime = new Showtime();
        showtime.setId(100L);
        showtime.setPublicId("showtime-1");
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setMovie(movie);
        showtime.setStartTime(Instant.parse("2026-08-08T12:00:00Z"));
        showtime.setEndTime(Instant.parse("2026-08-08T14:00:00Z"));

        SeatType seatType = new SeatType();
        seatType.setCode(SeatTypeCode.COUPLE);
        seatType.setName("Ghế đôi");
        coupleSeats = List.of(
                seat(1L, "seat-1", "I1", 1, "I-01", auditorium, seatType),
                seat(2L, "seat-2", "I2", 2, "I-01", auditorium, seatType));
    }

    @Test
    void blockSeats_ExpandsCouplePairForDraftShowtime() {
        showtime.setStatus(ShowtimeStatus.DRAFT);
        when(showtimeRepository.findByPublicIdForUpdate("showtime-1")).thenReturn(Optional.of(showtime));
        when(seatRepository.findAdminLayoutByAuditoriumId(20L)).thenReturn(coupleSeats);
        when(blockedSeatRepository.findForUpdate(100L, List.of(1L, 2L))).thenReturn(List.of());
        when(blockedSeatRepository.findByShowtimeIdAndStatus(100L, com.lorafilm.movie.common.enums.ActionStatus.ACTIVE))
                .thenReturn(List.of());
        when(currentUserProvider.getCurrentUserId()).thenReturn(9L);

        service.blockSeats(
                "showtime-1",
                new UpdateShowtimeBlockedSeatsRequest(List.of("seat-1"), "Hỏng tay ghế"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ShowtimeBlockedSeat>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(blockedSeatRepository).saveAll(captor.capture());
        List<ShowtimeBlockedSeat> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertEquals(2, saved.size());
        assertEquals(List.of("I1", "I2"), saved.stream().map(row -> row.getSeat().getSeatCode()).toList());
        assertEquals(List.of("Hỏng tay ghế", "Hỏng tay ghế"), saved.stream().map(ShowtimeBlockedSeat::getReason).toList());
        verify(bookingAvailabilityClient, never()).check(100L, List.of(1L, 2L));
    }

    @Test
    void blockSeats_RejectsPairWhenCustomerAlreadyHasOneSeat() {
        showtime.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        when(showtimeRepository.findByPublicIdForUpdate("showtime-1")).thenReturn(Optional.of(showtime));
        when(seatRepository.findAdminLayoutByAuditoriumId(20L)).thenReturn(coupleSeats);
        when(blockedSeatRepository.findForUpdate(100L, List.of(1L, 2L))).thenReturn(List.of());
        when(bookingAvailabilityClient.check(100L, List.of(1L, 2L)))
                .thenReturn(new BookingSeatAvailabilityClient.AvailabilityResult(true, List.of(2L)));

        BusinessException error = assertThrows(BusinessException.class, () -> service.blockSeats(
                "showtime-1",
                new UpdateShowtimeBlockedSeatsRequest(List.of("seat-1"), "Khóa để sửa ghế")));

        assertEquals(ErrorCode.SHOWTIME_SEAT_ALREADY_OCCUPIED, error.getErrorCode());
        verify(blockedSeatRepository, never()).saveAll(anyList());
    }

    @Test
    void blockSeats_RejectsShowtimeThatAlreadyStarted() {
        showtime.setStatus(ShowtimeStatus.DRAFT);
        showtime.setStartTime(Instant.parse("2026-08-08T07:59:59Z"));
        when(showtimeRepository.findByPublicIdForUpdate("showtime-1")).thenReturn(Optional.of(showtime));

        BusinessException error = assertThrows(BusinessException.class, () -> service.blockSeats(
                "showtime-1",
                new UpdateShowtimeBlockedSeatsRequest(List.of("seat-1"), "Hỏng ghế")));

        assertEquals(ErrorCode.SHOWTIME_SEAT_CONTROL_NOT_EDITABLE, error.getErrorCode());
        verify(seatRepository, never()).findAdminLayoutByAuditoriumId(20L);
    }

    private Seat seat(
            Long id,
            String publicId,
            String code,
            int positionColumn,
            String pairGroup,
            Auditorium auditorium,
            SeatType seatType) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setPublicId(publicId);
        seat.setSeatCode(code);
        seat.setRowLabel("I");
        seat.setSeatNumber(positionColumn);
        seat.setPositionRow(9);
        seat.setPositionColumn(positionColumn);
        seat.setPairGroup(pairGroup);
        seat.setAuditorium(auditorium);
        seat.setSeatType(seatType);
        seat.setStatus(SeatStatus.ACTIVE);
        return seat;
    }
}

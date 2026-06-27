package com.project.bookingservice.service;

import com.project.bookingservice.config.BookingProperties;
import com.project.bookingservice.dto.movie.SeatInfo;
import com.project.bookingservice.dto.movie.ShowtimeInfo;
import com.project.bookingservice.dto.reservation.CreateReservationRequest;
import com.project.bookingservice.dto.reservation.ReservationResponse;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.exception.BusinessException;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.security.CurrentUserProvider;
import com.project.bookingservice.service.idempotency.IdempotencyService;
import com.project.bookingservice.service.lock.SeatLockManager;
import com.project.bookingservice.service.movie.MovieServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceTest {

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @Mock
    private MovieServiceClient movieServiceClient;

    @Mock
    private SeatLockManager seatLockManager;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private BookingProperties bookingProperties;

    @InjectMocks
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(currentUserProvider.getCurrentUserId()).thenReturn(15L);

        BookingProperties.Reservation resProps = new BookingProperties.Reservation();
        resProps.setTtlSeconds(300);
        when(bookingProperties.getReservation()).thenReturn(resProps);
    }

    @Test
    void createReservation_EmptySeatList_ThrowsException() {
        CreateReservationRequest request = new CreateReservationRequest(10L, new ArrayList<>());
        
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> reservationService.createReservation(request, "idemp-key"));
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
    }

    @Test
    void createReservation_DuplicateSeats_ThrowsException() {
        CreateReservationRequest request = new CreateReservationRequest(10L, Arrays.asList(101L, 101L));
        
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> reservationService.createReservation(request, "idemp-key"));
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
    }

    @Test
    void createReservation_IdempotencyReplay_ReturnsPreviousResponse() {
        CreateReservationRequest request = new CreateReservationRequest(10L, Arrays.asList(101L));
        when(idempotencyService.hasKey(15L, "idemp-key")).thenReturn(true);
        
        List<ReservationResponse> prevResponses = Arrays.asList(new ReservationResponse());
        when(idempotencyService.getResponse(eq(15L), eq("idemp-key"), eq(request))).thenReturn(prevResponses);

        List<ReservationResponse> responses = reservationService.createReservation(request, "idemp-key");
        assertEquals(prevResponses, responses);
    }

    @Test
    void createReservation_IdempotencyConflict_ThrowsException() {
        CreateReservationRequest request = new CreateReservationRequest(10L, Arrays.asList(101L));
        when(idempotencyService.hasKey(15L, "idemp-key")).thenReturn(true);
        when(idempotencyService.getResponse(eq(15L), eq("idemp-key"), eq(request))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> reservationService.createReservation(request, "idemp-key"));
        assertEquals("BOOKING_IDEMPOTENCY_CONFLICT", ex.getErrorCode());
    }

    @Test
    void createReservation_InvalidShowtime_ThrowsException() {
        CreateReservationRequest request = new CreateReservationRequest(10L, Arrays.asList(101L));
        when(movieServiceClient.getShowtime(10L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> reservationService.createReservation(request, "idemp-key"));
        assertEquals("BOOKING_SHOWTIME_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void createReservation_Success() {
        CreateReservationRequest request = new CreateReservationRequest(10L, Arrays.asList(101L));
        when(idempotencyService.hasKey(15L, "idemp-key")).thenReturn(false);
        when(movieServiceClient.getShowtime(10L)).thenReturn(new ShowtimeInfo(10L, 1L, true));
        when(movieServiceClient.getSeats(request.getSeatIds())).thenReturn(Arrays.asList(new SeatInfo(101L, 1L, true)));
        when(movieServiceClient.isSeatBooked(10L, 101L)).thenReturn(false);
        when(seatReservationRepository.findActiveReservations(10L, request.getSeatIds()))
                .thenReturn(new ArrayList<>());
        when(seatLockManager.acquireLocks(10L, request.getSeatIds(), "15")).thenReturn(true);
        when(seatReservationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ReservationResponse> responses = reservationService.createReservation(request, "idemp-key");
        
        assertEquals(1, responses.size());
        verify(seatReservationRepository).saveAll(anyList());
        verify(idempotencyService).saveResponse(eq(15L), eq("idemp-key"), eq(request), anyList());
    }
}

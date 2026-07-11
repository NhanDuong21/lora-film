package com.project.bookingservice.service.impl;

import com.project.bookingservice.dto.movie.ShowtimeInfo;
import com.project.bookingservice.dto.request.CreateBookingRequest;
import com.project.bookingservice.dto.response.BookingResponse;
import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.exception.BusinessException;
import com.project.bookingservice.repository.BookingRepository;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.security.CurrentUserProvider;
import com.project.bookingservice.service.lock.SeatLockManager;
import com.project.bookingservice.service.movie.MovieServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SeatReservationRepository seatReservationRepository;


    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private MovieServiceClient movieServiceClient;
    @Mock
    private com.project.bookingservice.repository.TicketRepository ticketRepository;
    
    private SeatLockManager seatLockManager = new SeatLockManager(null, null) {
        @Override
        public boolean acquireLocks(Long showtimeId, List<Long> seatIds, String lockOwner) {
            return mockAcquireLocks(showtimeId, seatIds, lockOwner);
        }

        @Override
        public void releaseLocks(Long showtimeId, List<Long> seatIds, String lockOwner) {
            mockReleaseLocks(showtimeId, seatIds, lockOwner);
        }
    };

    private BookingServiceImpl bookingService;


    private int releaseLocksCount = 0;

    protected boolean mockAcquireLocks(Long showtimeId, List<Long> seatIds, String lockOwner) {
        return true;
    }

    protected void mockReleaseLocks(Long showtimeId, List<Long> seatIds, String lockOwner) {
        releaseLocksCount++;
    }



    private final Long userId = 40L;
    private final String idempotencyKey = "test-key";
    private final Long showtimeId = 100L;
    private final BigDecimal ticketPrice = new BigDecimal("10.00");

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        bookingService = new BookingServiceImpl(bookingRepository, seatReservationRepository, currentUserProvider, movieServiceClient, seatLockManager, ticketRepository);
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        releaseLocksCount = 0;
    }

    // TEST 1: Create booking with one reservation
    @Test
    public void testCreateBooking_OneReservation_Success() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res.setId(1L);
        res.setStatus(ReservationStatus.HELD);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res));
        
        ShowtimeInfo showtime = new ShowtimeInfo();
        showtime.setId(showtimeId);
        showtime.setPrice(ticketPrice);
        when(movieServiceClient.getShowtime(showtimeId)).thenReturn(showtime);
        
        when(bookingRepository.existsByBookingCode(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(500L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request, idempotencyKey);
        
        assertNotNull(response);
        assertEquals(500L, response.getBookingId());
        assertEquals(BookingStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(0, ticketPrice.compareTo(response.getTotalAmount()));
        
        assertEquals(ReservationStatus.CONVERTED, res.getStatus());
        assertEquals(500L, res.getBookingId());
    }

    // TEST 2: Create booking with multiple reservations
    @Test
    public void testCreateBooking_MultipleReservations_Success() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L, 2L));
        SeatReservation res1 = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res1.setId(1L); res1.setStatus(ReservationStatus.HELD);
        SeatReservation res2 = new SeatReservation(showtimeId, 11L, userId, LocalDateTime.now().plusMinutes(5));
        res2.setId(2L); res2.setStatus(ReservationStatus.HELD);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res1, res2));
        
        ShowtimeInfo showtime = new ShowtimeInfo();
        showtime.setId(showtimeId);
        showtime.setPrice(ticketPrice);
        when(movieServiceClient.getShowtime(showtimeId)).thenReturn(showtime);
        
        when(bookingRepository.existsByBookingCode(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(500L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request, idempotencyKey);
        
        assertNotNull(response);
        assertEquals(0, new BigDecimal("20.00").compareTo(response.getTotalAmount()));
        assertEquals(ReservationStatus.CONVERTED, res1.getStatus());
        assertEquals(ReservationStatus.CONVERTED, res2.getStatus());
    }

    // TEST 3: Reject reservation from another user
    @Test
    public void testCreateBooking_OtherUserReservation_ThrowsException() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        SeatReservation res = new SeatReservation(showtimeId, 10L, 999L, LocalDateTime.now().plusMinutes(5));
        res.setId(1L); res.setStatus(ReservationStatus.HELD);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res));
        
        BusinessException e = assertThrows(BusinessException.class, () -> bookingService.createBooking(request, idempotencyKey));
        assertEquals("SEAT_RESERVATION_OWNERSHIP_MISMATCH", e.getErrorCode());
    }

    // TEST 4: Reject mixed showtime reservations
    @Test
    public void testCreateBooking_MixedShowtime_ThrowsException() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L, 2L));
        SeatReservation res1 = new SeatReservation(100L, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res1.setId(1L); res1.setStatus(ReservationStatus.HELD);
        SeatReservation res2 = new SeatReservation(101L, 11L, userId, LocalDateTime.now().plusMinutes(5));
        res2.setId(2L); res2.setStatus(ReservationStatus.HELD);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res1, res2));
        
        BusinessException e = assertThrows(BusinessException.class, () -> bookingService.createBooking(request, idempotencyKey));
        assertEquals("BOOKING_MULTIPLE_SHOWTIMES_NOT_ALLOWED", e.getErrorCode());
    }

    // TEST 5: Reject expired reservation
    @Test
    public void testCreateBooking_ExpiredReservation_ThrowsException() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().minusMinutes(5));
        res.setId(1L); res.setStatus(ReservationStatus.HELD);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res));
        
        BusinessException e = assertThrows(BusinessException.class, () -> bookingService.createBooking(request, idempotencyKey));
        assertEquals("SEAT_RESERVATION_EXPIRED", e.getErrorCode());
    }

    // TEST 6: Reject invalid reservation status
    @Test
    public void testCreateBooking_InvalidStatus_ThrowsException() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res.setId(1L); res.setStatus(ReservationStatus.RELEASED);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res));
        
        BusinessException e = assertThrows(BusinessException.class, () -> bookingService.createBooking(request, idempotencyKey));
        assertEquals("BOOKING_INVALID_STATUS", e.getErrorCode());
    }

    // TEST 7: Verify reservation status HELD -> CONVERTED (done in Test 1)
    // TEST 8: Verify reservation.booking_id assigned (done in Test 1)
    
    // TEST 9: Verify no ticket created during booking
    @Test
    public void testCreateBooking_NoTicketsCreated() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res.setId(1L); res.setStatus(ReservationStatus.HELD);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res));
        
        ShowtimeInfo showtime = new ShowtimeInfo();
        showtime.setId(showtimeId); showtime.setPrice(ticketPrice);
        when(movieServiceClient.getShowtime(showtimeId)).thenReturn(showtime);
        
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0); b.setId(500L); return b;
        });

        BookingResponse response = bookingService.createBooking(request, idempotencyKey);
        assertNull(response.getTickets());
    }


    @Test
    public void testCancelBooking_Success() {
        Booking booking = new Booking();
        booking.setId(500L);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setShowtimeId(showtimeId);
        
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res.setId(1L); res.setSeatId(10L);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(seatReservationRepository.findByBookingId(500L)).thenReturn(Arrays.asList(res));
        
        bookingService.cancelBooking(500L, idempotencyKey);
        
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
        assertEquals(1, releaseLocksCount);
    }

    // TEST 13: Reject cancel CONFIRMED booking
    @Test
    public void testCancelBooking_Confirmed_ThrowsException() {
        Booking booking = new Booking();
        booking.setId(500L);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        
        BusinessException e = assertThrows(BusinessException.class, () -> bookingService.cancelBooking(500L, idempotencyKey));
        assertEquals("BOOKING_CANNOT_BE_CANCELLED", e.getErrorCode());
    }

    // TEST 14: Optimistic lock conflict
    // Simulating optimistic lock conflict is best done in integration tests, but we could mock a thrown exception.
    
    // TEST 15: Movie Service unavailable
    @Test
    public void testCreateBooking_MovieServiceUnavailable_ThrowsException() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res.setId(1L); res.setStatus(ReservationStatus.HELD);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res));
        
        when(movieServiceClient.getShowtime(showtimeId)).thenThrow(new RuntimeException("Service down"));
        
        BusinessException e = assertThrows(BusinessException.class, () -> bookingService.createBooking(request, idempotencyKey));
        assertEquals("MOVIE_SERVICE_UNAVAILABLE", e.getErrorCode());
    }

    // TEST 16: Booking code uniqueness collision handling
    @Test
    public void testCreateBooking_CodeCollisionRetry() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res.setId(1L); res.setStatus(ReservationStatus.HELD);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res));
        
        ShowtimeInfo showtime = new ShowtimeInfo();
        showtime.setId(showtimeId); showtime.setPrice(ticketPrice);
        when(movieServiceClient.getShowtime(showtimeId)).thenReturn(showtime);
        
        when(bookingRepository.existsByBookingCode(anyString())).thenReturn(true).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0); b.setId(500L); return b;
        });

        BookingResponse response = bookingService.createBooking(request, idempotencyKey);
        
        assertNotNull(response);
        verify(bookingRepository, times(2)).existsByBookingCode(anyString());
    }

    // Admin methods tests
    @Test
    public void testUpdateBookingStatusAdmin_Success() {
        Booking booking = new Booking();
        booking.setId(500L);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setShowtimeId(showtimeId);
        
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res.setId(1L); res.setSeatId(10L);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(seatReservationRepository.findByBookingId(500L)).thenReturn(Arrays.asList(res));
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        
        bookingService.updateBookingStatusAdmin(500L, BookingStatus.CANCELLED, "Admin cancelled");
        
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
        assertEquals(1, releaseLocksCount);
    }

    @Test
    public void testUpdateBookingStatusAdmin_InvalidStatus_ThrowsException() {
        Booking booking = new Booking();
        booking.setId(500L);
        booking.setStatus(BookingStatus.EXPIRED);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        
        BusinessException e = assertThrows(BusinessException.class, () -> bookingService.updateBookingStatusAdmin(500L, BookingStatus.CANCELLED, "Admin cancelled"));
        assertEquals("BOOKING_ALREADY_EXPIRED", e.getErrorCode());
    }
}

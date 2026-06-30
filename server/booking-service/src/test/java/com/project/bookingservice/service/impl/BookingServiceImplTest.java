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
import com.project.bookingservice.service.idempotency.IdempotencyService;
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
    private IdempotencyService idempotencyService = new IdempotencyService(null, null, null) {
        @Override
        public boolean acquireIdempotency(Long userId, String idempotencyKey, Object requestPayload) {
            return mockAcquireIdempotency(userId, idempotencyKey, requestPayload);
        }
        
        @Override
        public void saveResponse(Long userId, String idempotencyKey, Object requestPayload, Object response) {
            mockSaveResponse(userId, idempotencyKey, requestPayload, response);
        }

        @Override
        public void removeIdempotencyKey(Long userId, String idempotencyKey) {
            mockRemoveIdempotencyKey(userId, idempotencyKey);
        }

        @Override
        public IdempotencyRecord getIdempotencyRecord(Long userId, String idempotencyKey) {
            return mockGetIdempotencyRecord(userId, idempotencyKey);
        }
        
        @Override
        public String generateHash(Object requestPayload) {
            return "hash";
        }
    };

    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private MovieServiceClient movieServiceClient;
    
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

    private boolean acquireIdempotencyResult = true;
    private IdempotencyService.IdempotencyRecord idempotencyRecordResult = null;
    private int saveResponseCount = 0;
    private int removeIdempotencyKeyCount = 0;
    private int releaseLocksCount = 0;

    protected boolean mockAcquireLocks(Long showtimeId, List<Long> seatIds, String lockOwner) {
        return true;
    }

    protected void mockReleaseLocks(Long showtimeId, List<Long> seatIds, String lockOwner) {
        releaseLocksCount++;
    }

    protected boolean mockAcquireIdempotency(Long userId, String idempotencyKey, Object requestPayload) {
        return acquireIdempotencyResult;
    }

    protected void mockSaveResponse(Long userId, String idempotencyKey, Object requestPayload, Object response) {
        saveResponseCount++;
    }

    protected void mockRemoveIdempotencyKey(Long userId, String idempotencyKey) {
        removeIdempotencyKeyCount++;
    }

    protected IdempotencyService.IdempotencyRecord mockGetIdempotencyRecord(Long userId, String idempotencyKey) {
        return idempotencyRecordResult;
    }

    private final Long userId = 40L;
    private final String idempotencyKey = "test-key";
    private final Long showtimeId = 100L;
    private final BigDecimal ticketPrice = new BigDecimal("10.00");

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        bookingService = new BookingServiceImpl(bookingRepository, seatReservationRepository, idempotencyService, currentUserProvider, movieServiceClient, seatLockManager);
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        acquireIdempotencyResult = true;
        idempotencyRecordResult = null;
        saveResponseCount = 0;
        removeIdempotencyKeyCount = 0;
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
        
        assertEquals(1, saveResponseCount);
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
        assertEquals("BOOKING_RESERVATION_NOT_OWNED", e.getErrorCode());
        assertEquals(1, removeIdempotencyKeyCount);
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
        assertEquals("BOOKING_RESERVATION_SHOWTIME_MISMATCH", e.getErrorCode());
    }

    // TEST 5: Reject expired reservation
    @Test
    public void testCreateBooking_ExpiredReservation_ThrowsException() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().minusMinutes(5));
        res.setId(1L); res.setStatus(ReservationStatus.HELD);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res));
        
        BusinessException e = assertThrows(BusinessException.class, () -> bookingService.createBooking(request, idempotencyKey));
        assertEquals("BOOKING_RESERVATION_EXPIRED", e.getErrorCode());
    }

    // TEST 6: Reject invalid reservation status
    @Test
    public void testCreateBooking_InvalidStatus_ThrowsException() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        SeatReservation res = new SeatReservation(showtimeId, 10L, userId, LocalDateTime.now().plusMinutes(5));
        res.setId(1L); res.setStatus(ReservationStatus.RELEASED);
        when(seatReservationRepository.findAllById(request.getReservationIds())).thenReturn(Arrays.asList(res));
        
        BusinessException e = assertThrows(BusinessException.class, () -> bookingService.createBooking(request, idempotencyKey));
        assertEquals("BOOKING_RESERVATION_INVALID_STATUS", e.getErrorCode());
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

    // TEST 10: Retry create request same idempotency key
    @Test
    public void testCreateBooking_IdempotencyReplay() {
        CreateBookingRequest request = new CreateBookingRequest(Arrays.asList(1L));
        acquireIdempotencyResult = false;
        
        IdempotencyService.IdempotencyRecord record = new IdempotencyService.IdempotencyRecord();
        record.setRequestHash("hash");
        
        BookingResponse mockResponse = new BookingResponse();
        mockResponse.setBookingId(500L);
        record.setResponse(mockResponse);
        idempotencyRecordResult = record;
        
        BookingResponse response = bookingService.createBooking(request, idempotencyKey);
        assertEquals(500L, response.getBookingId());
        verify(bookingRepository, never()).save(any());
    }

    // TEST 11: Cancel valid booking (PENDING_PAYMENT -> CANCELLED)
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
        assertEquals("BOOKING_CANNOT_CANCEL_CONFIRMED", e.getErrorCode());
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
        assertEquals("BOOKING_PRICE_UNAVAILABLE", e.getErrorCode());
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
}

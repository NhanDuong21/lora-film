package com.project.bookingservice.service;

import com.project.bookingservice.dto.movie.ShowtimeInfo;
import com.project.bookingservice.dto.payment.PaymentContextResponse;
import com.project.bookingservice.dto.payment.PaymentResultRequest;
import com.project.bookingservice.dto.payment.PaymentResultResponse;
import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.entity.BookingPaymentResultEvent;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.entity.Ticket;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.exception.BusinessException;
import com.project.bookingservice.repository.BookingPaymentResultEventRepository;
import com.project.bookingservice.repository.BookingRepository;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.repository.TicketRepository;
import com.project.bookingservice.service.lock.SeatLockManager;
import com.project.bookingservice.service.movie.MovieServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class InternalPaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SeatReservationRepository seatReservationRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private SeatLockManager seatLockManager;
    @Mock
    private BookingPaymentResultEventRepository eventRepository;
    @Mock
    private MovieServiceClient movieServiceClient;

    @InjectMocks
    private InternalPaymentService internalPaymentService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetPaymentContext_Success() {
        Long bookingId = 1L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUserId(2L);
        booking.setShowtimeId(3L);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setTotalAmount(new BigDecimal("100000.00"));
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        ShowtimeInfo showtime = new ShowtimeInfo();
        showtime.setMovieId(10L);
        showtime.setMovieTitle("Avengers");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(movieServiceClient.getShowtime(3L)).thenReturn(showtime);
        when(seatReservationRepository.findByBookingIdAndStatus(bookingId, ReservationStatus.CONVERTED))
                .thenReturn(Collections.singletonList(new SeatReservation()));

        PaymentContextResponse response = internalPaymentService.getPaymentContext(bookingId);

        assertNotNull(response);
        assertEquals(bookingId, response.getBookingId());
        assertEquals(2L, response.getAccountId());
        assertTrue(response.isPayable());
        assertEquals(new BigDecimal("100000.00"), response.getAmount());
        assertEquals("VND", response.getCurrency());
        assertNotNull(response.getAnalyticsSnapshot());
        assertEquals(10L, response.getAnalyticsSnapshot().getMovieId());
        assertEquals("Avengers", response.getAnalyticsSnapshot().getMovieTitle());
        assertEquals(1, response.getAnalyticsSnapshot().getTicketCount());
    }

    @Test
    public void testGetPaymentContext_NotPayable() {
        Long bookingId = 1L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(10));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            internalPaymentService.getPaymentContext(bookingId);
        });

        assertEquals("BOOKING_NOT_PAYABLE", exception.getErrorCode());
    }

    @Test
    public void testProcessPaymentResult_AlreadyProcessed() {
        Long bookingId = 1L;
        PaymentResultRequest request = new PaymentResultRequest();
        request.setEventId(UUID.randomUUID().toString());
        request.setResult("SUCCESS");

        when(eventRepository.findByEventId(request.getEventId()))
                .thenReturn(Optional.of(new BookingPaymentResultEvent()));

        PaymentResultResponse response = internalPaymentService.processPaymentResult(bookingId, request);

        assertNotNull(response);
        assertEquals(request.getEventId(), response.getEventId());
        assertFalse(response.isApplied());
        assertTrue(response.isDuplicate());
        assertEquals("ALREADY_PROCESSED", response.getResult());
        
        verify(bookingRepository, never()).findByIdWithPessimisticLock(any());
    }

    @Test
    public void testProcessPaymentResult_Success() {
        Long bookingId = 1L;
        PaymentResultRequest request = new PaymentResultRequest();
        request.setEventId(UUID.randomUUID().toString());
        request.setResult("SUCCESS");
        request.setAmount(new BigDecimal("100000.00"));
        request.setCurrency("VND");
        request.setPaymentId(100L);
        request.setPaymentTransactionCode("PAY-100");
        request.setPaymentMethod("VNPAY");
        request.setReconciliationStatus("NONE");
        request.setOccurredAt(LocalDateTime.now());

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setTotalAmount(new BigDecimal("100000.00"));

        when(eventRepository.findByEventId(request.getEventId())).thenReturn(Optional.empty());
        when(bookingRepository.findByIdWithPessimisticLock(bookingId)).thenReturn(Optional.of(booking));
        when(seatReservationRepository.findByBookingIdAndStatus(bookingId, ReservationStatus.CONVERTED))
                .thenReturn(Collections.emptyList());

        PaymentResultResponse response = internalPaymentService.processPaymentResult(bookingId, request);

        assertNotNull(response);
        assertTrue(response.isApplied());
        assertFalse(response.isDuplicate());
        assertEquals("BOOKING_CONFIRMED", response.getResult());
        
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository).save(booking);
        verify(eventRepository).save(any(BookingPaymentResultEvent.class));
        verify(ticketRepository).saveAll(any());
    }

    @Test
    public void testProcessPaymentResult_AlreadyConfirmed() {
        Long bookingId = 1L;
        PaymentResultRequest request = new PaymentResultRequest();
        request.setEventId(UUID.randomUUID().toString());
        request.setResult("SUCCESS");
        request.setAmount(new BigDecimal("100000.00"));
        request.setCurrency("VND");
        request.setOccurredAt(LocalDateTime.now());

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("100000.00"));

        when(eventRepository.findByEventId(request.getEventId())).thenReturn(Optional.empty());
        when(bookingRepository.findByIdWithPessimisticLock(bookingId)).thenReturn(Optional.of(booking));

        PaymentResultResponse response = internalPaymentService.processPaymentResult(bookingId, request);

        assertNotNull(response);
        assertFalse(response.isApplied());
        assertFalse(response.isDuplicate());
        assertEquals("ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT", response.getResult());
        
        verify(bookingRepository, never()).save(any());
        verify(eventRepository).save(any(BookingPaymentResultEvent.class));
    }

    @Test
    public void testProcessPaymentResult_Failed() {
        Long bookingId = 1L;
        PaymentResultRequest request = new PaymentResultRequest();
        request.setEventId(UUID.randomUUID().toString());
        request.setResult("FAILED");
        request.setAmount(new BigDecimal("100000.00"));
        request.setCurrency("VND");
        request.setOccurredAt(LocalDateTime.now());

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setTotalAmount(new BigDecimal("100000.00"));

        when(eventRepository.findByEventId(request.getEventId())).thenReturn(Optional.empty());
        when(bookingRepository.findByIdWithPessimisticLock(bookingId)).thenReturn(Optional.of(booking));

        PaymentResultResponse response = internalPaymentService.processPaymentResult(bookingId, request);

        assertNotNull(response);
        assertFalse(response.isApplied());
        assertFalse(response.isDuplicate());
        assertEquals("ALREADY_PROCESSED", response.getResult());
        
        verify(bookingRepository, never()).save(any());
        verify(eventRepository).save(any(BookingPaymentResultEvent.class));
    }
}

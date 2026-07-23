package com.lorafilm.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.dto.BookingPriceSnapshotPayload;
import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.repository.BookingPriceSnapshotRepository;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.impl.InternalBookingPaymentServiceImpl;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.payment.entity.BookingPaymentEvent;
import com.lorafilm.booking.payment.enums.PaymentEventType;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalBookingPaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingPriceSnapshotRepository snapshotRepository;
    @Mock
    private BookingPaymentEventRepository eventRepository;
    @Mock
    private BookingStatusHistoryService historyService;

    private InternalBookingPaymentServiceImpl service;
    private ObjectMapper objectMapper;
    private Booking booking;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new InternalBookingPaymentServiceImpl(
                bookingRepository, snapshotRepository, eventRepository, historyService, objectMapper);
        booking = Booking.create(
                "550e8400-e29b-41d4-a716-446655440000",
                "LORAFILM-1",
                15L,
                1001L,
                101L,
                201L,
                301L,
                new BigDecimal("240000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "VND",
                Instant.now().plusSeconds(900),
                null);
        booking.setId(100L);
    }

    @Test
    void returnsStoredBookingAmountAndSnapshotAnalytics() throws Exception {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(snapshotRepository.findByBookingId(100L)).thenReturn(Optional.of(snapshot()));

        var context = service.getPaymentContext(100L);

        assertEquals(new BigDecimal("240000.00"), context.amount());
        assertEquals("VND", context.currency());
        assertTrue(context.payable());
        assertEquals("Superman", context.analyticsSnapshot().movieTitle());
        assertEquals(2, context.analyticsSnapshot().ticketCount());
    }

    @Test
    void rejectsPaymentResultAmountMismatch() {
        when(eventRepository.findByPublicId("event-1")).thenReturn(Optional.empty());
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        InternalPaymentResultRequest request = result("event-1", "SUCCESS", new BigDecimal("239999"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.recordPaymentResult(100L, request));

        assertEquals("PAYMENT_AMOUNT_MISMATCH", exception.getErrorCode());
    }

    @Test
    void confirmsBookingAndPersistsIdempotencyEvent() {
        when(eventRepository.findByPublicId("event-1")).thenReturn(Optional.empty());
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        InternalPaymentResultRequest request = result("event-1", "SUCCESS", new BigDecimal("240000.00"));

        var response = service.recordPaymentResult(100L, request);

        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(PaymentStatus.SUCCESS, booking.getPaymentStatus());
        assertFalse(response.idempotent());
        verify(eventRepository).save(any(BookingPaymentEvent.class));
    }

    @Test
    void replaysSameEventWithoutApplyingStatusAgain() {
        BookingPaymentEvent event = new BookingPaymentEvent();
        event.setPublicId("event-1");
        event.setBooking(booking);
        event.setPaymentId(900L);
        event.setPaymentMethod("MOCK");
        event.setEventType(PaymentEventType.PAYMENT_SUCCESS);
        event.setAmount(new BigDecimal("240000.00"));
        event.setCurrency("VND");
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        when(eventRepository.findByPublicId("event-1")).thenReturn(Optional.of(event));

        var response = service.recordPaymentResult(
                100L, result("event-1", "SUCCESS", new BigDecimal("240000.00")));

        assertTrue(response.idempotent());
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getBookingStatus());
    }

    private BookingPriceSnapshot snapshot() throws Exception {
        BookingPriceSnapshotPayload payload = new BookingPriceSnapshotPayload(
                1001L,
                "showtime-1",
                Instant.now(),
                "VND",
                101L,
                "Superman",
                new BigDecimal("240000"),
                List.of(
                        new BookingPriceSnapshotPayload.SeatPriceLine(
                                1L, "A01", "VIP", new BigDecimal("120000")),
                        new BookingPriceSnapshotPayload.SeatPriceLine(
                                2L, "A02", "VIP", new BigDecimal("120000"))));
        BookingPriceSnapshot snapshot = new BookingPriceSnapshot();
        snapshot.setBooking(booking);
        snapshot.setCurrency("VND");
        snapshot.setPricingBreakdownJson(objectMapper.writeValueAsString(payload));
        return snapshot;
    }

    private InternalPaymentResultRequest result(String eventId, String result, BigDecimal amount) {
        return new InternalPaymentResultRequest(
                eventId,
                "1.0",
                900L,
                "PAY-900",
                "MOCK",
                result,
                amount,
                "VND",
                LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC),
                "EXT-900",
                "MATCHED");
    }
}

package com.lorafilm.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.dto.BookingPriceSnapshotPayload;
import com.lorafilm.booking.booking.client.ScoreRedemptionClient;
import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.repository.BookingPriceSnapshotRepository;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingLifecycleService;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.impl.InternalBookingPaymentServiceImpl;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.PaymentResultConflictException;
import com.lorafilm.booking.infrastructure.entity.BookingReconciliationTask;
import com.lorafilm.booking.infrastructure.repository.BookingReconciliationTaskRepository;
import com.lorafilm.booking.payment.entity.BookingPaymentEvent;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import com.lorafilm.booking.payment.repository.BookingRefundRepository;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalBookingPaymentServiceTest {

    private static final String BOOKING_PUBLIC_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String PAYMENT_PUBLIC_ID = "660e8400-e29b-41d4-a716-446655440000";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingPriceSnapshotRepository snapshotRepository;
    @Mock
    private BookingPaymentEventRepository eventRepository;
    @Mock
    private BookingStatusHistoryService historyService;
    @Mock
    private com.lorafilm.booking.booking.service.BookingTicketService bookingTicketService;
    @Mock
    private com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService;
    @Mock
    private com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager metricsManager;
    @Mock
    private SeatReservationRepository reservationRepository;
    @Mock
    private BookingReconciliationTaskRepository reconciliationTaskRepository;
    @Mock
    private BookingRefundRepository refundRepository;
    @Mock
    private BookingLifecycleService lifecycleService;
    @Mock
    private ScoreRedemptionClient scoreRedemptionClient;

    private InternalBookingPaymentServiceImpl service;
    private ObjectMapper objectMapper;
    private Booking booking;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new InternalBookingPaymentServiceImpl(
                bookingRepository,
                snapshotRepository,
                eventRepository,
                historyService,
                objectMapper,
                bookingTicketService,
                outboxService,
                metricsManager,
                reservationRepository,
                reconciliationTaskRepository,
                refundRepository,
                lifecycleService);
        service.setScoreRedemptionClient(scoreRedemptionClient);
        booking = Booking.create(
                BOOKING_PUBLIC_ID,
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
        booking.lockAmount(Instant.now());
    }

    @Test
    void returnsStoredBookingAmountPublicIdentityAndUtcInstants() throws Exception {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(snapshotRepository.findByBookingId(100L)).thenReturn(Optional.of(snapshot()));
        when(reservationRepository.findAllByBookingId(100L))
                .thenReturn(List.of(heldReservation()));

        var context = service.getPaymentContext(100L);

        assertEquals(BOOKING_PUBLIC_ID, context.bookingPublicId());
        assertEquals(new BigDecimal("240000.00"), context.amount());
        assertEquals("VND", context.currency());
        assertEquals(booking.getAmountLockedAt(), context.amountLockedAt());
        assertEquals(booking.getExpiresAt(), context.expiresAt());
        assertTrue(context.payable());
        assertEquals("Superman", context.analyticsSnapshot().movieTitle());
        assertEquals("movie-public-101", context.analyticsSnapshot().moviePublicId());
        assertEquals("cinema-public-201", context.analyticsSnapshot().cinemaPublicId());
        assertEquals(2, context.analyticsSnapshot().ticketCount());
    }

    @Test
    void rejectsUnfinalizedBookingContext() {
        booking.setAmountLockedAt(null);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getPaymentContext(100L));

        assertEquals("BOOKING_AMOUNT_NOT_LOCKED", exception.getErrorCode());
    }

    @Test
    void reportsCancelledBookingContextWithCustomerSafeReason() {
        booking.changeStatus(BookingStatus.CANCELLED, Instant.now());
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getPaymentContext(100L));

        assertEquals("BOOKING_CANCELLED", exception.getErrorCode());
        assertEquals("Đơn đặt vé đã được hủy và ghế đã được trả lại", exception.getMessage());
        verify(reservationRepository, never()).findAllByBookingId(any());
    }

    @Test
    void reportsExpiredBookingContextWithoutExtendingDeadline() {
        Instant originalDeadline = Instant.now().minusSeconds(1);
        booking.setExpiresAt(originalDeadline);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getPaymentContext(100L));

        assertEquals("BOOKING_PAYMENT_DEADLINE_EXPIRED", exception.getErrorCode());
        assertEquals(originalDeadline, booking.getExpiresAt());
    }

    @Test
    void amountMismatchPersistsReceiptAndReconciliationWithoutChangingLifecycle() {
        InternalPaymentResultRequest request = result(
                UUID.randomUUID().toString(),
                "SUCCESS",
                new BigDecimal("239999"));
        when(eventRepository.findByPublicId(request.eventId())).thenReturn(Optional.empty());
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        when(reconciliationTaskRepository.save(any(BookingReconciliationTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResultConflictException exception = assertThrows(
                PaymentResultConflictException.class,
                () -> service.recordPaymentResult(100L, request));

        assertEquals("PAYMENT_AMOUNT_MISMATCH", exception.getErrorCode());
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getBookingStatus());
        verify(eventRepository).saveAndFlush(any(BookingPaymentEvent.class));
        verify(reconciliationTaskRepository).save(any(BookingReconciliationTask.class));
        verify(lifecycleService, never()).confirmPayment(any(), any(), anyString());
    }

    @Test
    void confirmsBookingAndPersistsOriginalResponse() {
        InternalPaymentResultRequest request = result(
                UUID.randomUUID().toString(),
                "SUCCESS",
                new BigDecimal("240000.00"));
        when(eventRepository.findByPublicId(request.eventId())).thenReturn(Optional.empty());
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        when(reservationRepository.findAllByBookingId(100L))
                .thenReturn(List.of(heldReservation()));
        when(lifecycleService.confirmPayment(any(Booking.class), any(Instant.class), anyString()))
                .thenAnswer(invocation -> {
                    Booking candidate = invocation.getArgument(0);
                    candidate.changeStatus(BookingStatus.CONFIRMED, invocation.getArgument(1));
                    return candidate;
                });

        var response = service.recordPaymentResult(100L, request);

        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(PaymentStatus.SUCCESS, booking.getPaymentStatus());
        assertEquals(BOOKING_PUBLIC_ID, response.bookingPublicId());
        assertEquals(PAYMENT_PUBLIC_ID, response.paymentPublicId());
        assertTrue(response.accepted());
        assertFalse(response.idempotent());
        ArgumentCaptor<BookingPaymentEvent> eventCaptor =
                ArgumentCaptor.forClass(BookingPaymentEvent.class);
        verify(eventRepository).saveAndFlush(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue().getPayloadHash());
        assertNotNull(eventCaptor.getValue().getResponsePayload());
    }

    @Test
    void commitsHeldScoreAfterAuthoritativePaymentSuccess() {
        booking.setAmountLockedAt(null);
        booking.applyScoreRedemption(50, new BigDecimal("50000"), "HOLD-1");
        booking.lockAmount(Instant.now());
        InternalPaymentResultRequest request = result(
                UUID.randomUUID().toString(),
                "SUCCESS",
                new BigDecimal("190000.00"));
        when(eventRepository.findByPublicId(request.eventId())).thenReturn(Optional.empty());
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        when(reservationRepository.findAllByBookingId(100L))
                .thenReturn(List.of(heldReservation()));
        when(lifecycleService.confirmPayment(any(Booking.class), any(Instant.class), anyString()))
                .thenAnswer(invocation -> {
                    Booking candidate = invocation.getArgument(0);
                    candidate.changeStatus(BookingStatus.CONFIRMED, invocation.getArgument(1));
                    return candidate;
                });

        service.recordPaymentResult(100L, request);

        verify(scoreRedemptionClient).commit(
                eq(100L),
                eq("HOLD-1"),
                eq(request.eventId()),
                anyString());
    }

    @Test
    void replaysExactEventFromStoredResponseWithoutApplyingLifecycleAgain() throws Exception {
        String eventId = UUID.randomUUID().toString();
        InternalPaymentResultRequest request = result(
                eventId,
                "SUCCESS",
                new BigDecimal("240000.00"));
        BookingPaymentEvent event = acceptedLegacyEvent(eventId, request);
        event.setResponsePayload(objectMapper.writeValueAsString(
                new com.lorafilm.booking.booking.dto.response.InternalPaymentResultResponse(
                        100L,
                        BOOKING_PUBLIC_ID,
                        null,
                        PAYMENT_PUBLIC_ID,
                        eventId,
                        "CONFIRMED",
                        "SUCCESS",
                        true,
                        false,
                        false,
                        null)));
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));

        var response = service.recordPaymentResult(100L, request);

        assertTrue(response.idempotent());
        assertEquals("CONFIRMED", response.bookingStatus());
        verify(lifecycleService, never()).confirmPayment(any(), any(), anyString());
    }

    @Test
    void sameEventWithChangedExternalReferenceConflicts() {
        String eventId = UUID.randomUUID().toString();
        InternalPaymentResultRequest original = result(
                eventId,
                "SUCCESS",
                new BigDecimal("240000.00"));
        BookingPaymentEvent event = acceptedLegacyEvent(eventId, original);
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));

        InternalPaymentResultRequest changed = new InternalPaymentResultRequest(
                original.eventId(),
                original.schemaVersion(),
                original.paymentId(),
                original.paymentPublicId(),
                original.paymentTransactionCode(),
                original.paymentProvider(),
                original.paymentMethod(),
                original.result(),
                original.amount(),
                original.currency(),
                original.occurredAt(),
                "DIFFERENT-EXTERNAL-ID");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.recordPaymentResult(100L, changed));

        assertEquals("PAYMENT_EVENT_ID_REUSED", exception.getErrorCode());
    }

    @Test
    void lateSuccessCreatesOneReplayableReconciliationReceipt() {
        booking.setExpiresAt(Instant.now().minusSeconds(1));
        String eventId = UUID.randomUUID().toString();
        InternalPaymentResultRequest request = result(
                eventId,
                "SUCCESS",
                new BigDecimal("240000.00"));
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        when(eventRepository.findByPublicId(eventId))
                .thenReturn(Optional.empty());
        when(reconciliationTaskRepository.save(any(BookingReconciliationTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResultConflictException first = assertThrows(
                PaymentResultConflictException.class,
                () -> service.recordPaymentResult(100L, request));

        assertEquals("LATE_PAYMENT_SUCCESS", first.getErrorCode());
        verify(eventRepository).saveAndFlush(any(BookingPaymentEvent.class));
        verify(reconciliationTaskRepository).save(any(BookingReconciliationTask.class));
    }

    @Test
    void failedAttemptDoesNotExtendDeadlineAndSuccessCanRetryBeforeIt() {
        Instant originalDeadline = booking.getExpiresAt();
        InternalPaymentResultRequest failed = result(
                UUID.randomUUID().toString(),
                "FAILED",
                new BigDecimal("240000.00"));
        InternalPaymentResultRequest success = result(
                UUID.randomUUID().toString(),
                "SUCCESS",
                new BigDecimal("240000.00"));
        when(bookingRepository.findByIdForPaymentUpdate(100L)).thenReturn(Optional.of(booking));
        when(eventRepository.findByPublicId(failed.eventId())).thenReturn(Optional.empty());
        when(eventRepository.findByPublicId(success.eventId())).thenReturn(Optional.empty());
        when(reservationRepository.findAllByBookingId(100L))
                .thenReturn(List.of(heldReservation()));
        when(lifecycleService.confirmPayment(any(Booking.class), any(Instant.class), anyString()))
                .thenAnswer(invocation -> {
                    Booking candidate = invocation.getArgument(0);
                    candidate.changeStatus(BookingStatus.CONFIRMED, invocation.getArgument(1));
                    return candidate;
                });

        service.recordPaymentResult(100L, failed);
        assertEquals(PaymentStatus.FAILED, booking.getPaymentStatus());
        assertEquals(originalDeadline, booking.getExpiresAt());

        service.recordPaymentResult(100L, success);
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(originalDeadline, booking.getExpiresAt());
    }

    @Test
    void refundSuccessUsesLifecycleAndNeverReleasesBookedCapacity() {
        booking.changeStatus(BookingStatus.CONFIRMED, Instant.now());
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        SeatReservation booked = heldReservation();
        booked.setStatus(SeatReservationStatus.BOOKED);
        InternalPaymentResultRequest refund = result(
                UUID.randomUUID().toString(),
                "REFUND_SUCCESS",
                new BigDecimal("240000.00"));
        when(bookingRepository.findByPublicIdWithLock(BOOKING_PUBLIC_ID))
                .thenReturn(Optional.of(booking));
        when(eventRepository.findByPublicId(refund.eventId())).thenReturn(Optional.empty());
        when(lifecycleService.transition(
                any(Booking.class),
                org.mockito.ArgumentMatchers.eq(BookingStatus.REFUNDED),
                anyString(),
                anyString()))
                .thenAnswer(invocation -> {
                    Booking candidate = invocation.getArgument(0);
                    candidate.changeStatus(BookingStatus.REFUNDED, Instant.now());
                    return candidate;
                });

        service.recordRefundResult(BOOKING_PUBLIC_ID, refund);

        assertEquals(BookingStatus.REFUNDED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertEquals(SeatReservationStatus.BOOKED, booked.getStatus());
        verify(reservationRepository, never()).saveAll(any());
        verify(refundRepository, atLeastOnce()).save(any());
    }

    @Test
    void partialRefundKeepsConfirmedBookingTicketsAndBookedCapacity() {
        booking.changeStatus(BookingStatus.CONFIRMED, Instant.now());
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        SeatReservation booked = heldReservation();
        booked.setStatus(SeatReservationStatus.BOOKED);
        InternalPaymentResultRequest refund = result(
                UUID.randomUUID().toString(),
                "REFUND_SUCCESS",
                new BigDecimal("50000.00"));
        when(bookingRepository.findByPublicIdWithLock(BOOKING_PUBLIC_ID))
                .thenReturn(Optional.of(booking));
        when(eventRepository.findByPublicId(refund.eventId())).thenReturn(Optional.empty());
        when(refundRepository.sumSuccessfulAmountByBookingId(booking.getId()))
                .thenReturn(BigDecimal.ZERO);

        service.recordRefundResult(BOOKING_PUBLIC_ID, refund);

        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(PaymentStatus.SUCCESS, booking.getPaymentStatus());
        assertEquals(SeatReservationStatus.BOOKED, booked.getStatus());
        verify(lifecycleService, never()).transition(
                any(), any(), anyString(), anyString());
        verify(reservationRepository, never()).saveAll(any());
        verify(refundRepository, atLeastOnce()).save(any());
    }

    @Test
    void cumulativePartialRefundBecomesFullOnlyAtOriginalOrderAmount() {
        booking.changeStatus(BookingStatus.CONFIRMED, Instant.now());
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        InternalPaymentResultRequest refund = result(
                UUID.randomUUID().toString(),
                "REFUND_SUCCESS",
                new BigDecimal("190000.00"));
        when(bookingRepository.findByPublicIdWithLock(BOOKING_PUBLIC_ID))
                .thenReturn(Optional.of(booking));
        when(eventRepository.findByPublicId(refund.eventId())).thenReturn(Optional.empty());
        when(refundRepository.sumSuccessfulAmountByBookingId(booking.getId()))
                .thenReturn(new BigDecimal("50000.00"));
        when(lifecycleService.transition(
                any(Booking.class),
                org.mockito.ArgumentMatchers.eq(BookingStatus.REFUNDED),
                anyString(),
                anyString()))
                .thenAnswer(invocation -> {
                    Booking candidate = invocation.getArgument(0);
                    candidate.changeStatus(BookingStatus.REFUNDED, Instant.now());
                    return candidate;
                });

        service.recordRefundResult(BOOKING_PUBLIC_ID, refund);

        assertEquals(BookingStatus.REFUNDED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        verify(lifecycleService).transition(
                any(Booking.class),
                org.mockito.ArgumentMatchers.eq(BookingStatus.REFUNDED),
                anyString(),
                anyString());
    }

    private BookingPriceSnapshot snapshot() throws Exception {
        BookingPriceSnapshotPayload payload = new BookingPriceSnapshotPayload(
                1001L,
                "showtime-1",
                Instant.now(),
                "VND",
                101L,
                "movie-public-101",
                "Superman",
                "cinema-public-201",
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

    private InternalPaymentResultRequest result(
            String eventId,
            String result,
            BigDecimal amount) {
        return new InternalPaymentResultRequest(
                eventId,
                "1.0",
                null,
                PAYMENT_PUBLIC_ID,
                "PAY-900",
                "VNPAY",
                "QR_CODE",
                result,
                amount,
                "VND",
                Instant.now(),
                "EXT-900");
    }

    private BookingPaymentEvent acceptedLegacyEvent(
            String eventId,
            InternalPaymentResultRequest request) {
        BookingPaymentEvent event = new BookingPaymentEvent();
        event.setPublicId(eventId);
        event.setBooking(booking);
        event.setPaymentId(request.paymentId());
        event.setPaymentPublicId(request.paymentPublicId());
        event.setSchemaVersion(request.schemaVersion());
        event.setTransactionId(request.paymentTransactionCode());
        event.setGatewayTransactionId(request.externalTransactionId());
        event.setPaymentProvider(request.paymentProvider());
        event.setPaymentMethod(request.paymentMethod());
        event.setEventType(com.lorafilm.booking.payment.enums.PaymentEventType.PAYMENT_SUCCESS);
        event.setAmount(request.amount());
        event.setCurrency(request.currency());
        event.setProcessingOutcome("ACCEPTED");
        event.setOccurredAt(request.occurredAt());
        return event;
    }

    private SeatReservation heldReservation() {
        SeatReservation reservation = new SeatReservation();
        reservation.setStatus(SeatReservationStatus.HELD);
        reservation.setExpiresAt(booking.getExpiresAt());
        return reservation;
    }
}

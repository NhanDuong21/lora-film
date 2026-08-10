package com.lorafilm.booking.service;

import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.dto.BookingSnapshotDto;
import com.lorafilm.booking.booking.dto.BookingOperationsSummaryResponse;
import com.lorafilm.booking.booking.dto.UpdateBookingStatusRequest;
import com.lorafilm.booking.booking.client.ShowtimeClient;
import com.lorafilm.booking.booking.client.ShowtimePresentationContext;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingSnapshotService;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.BookingStatusTransitionService;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.booking.service.impl.AdminBookingServiceImpl;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.response.PagedResponse;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingStatusHistoryService historyService;
    @Mock
    private BookingAuditService auditService;
    @Mock
    private BookingOperationLogService operationLogService;
    @Mock
    private BookingOutboxService outboxService;
    @Mock
    private BookingTicketService ticketService;
    @Mock
    private BookingSnapshotService snapshotService;
    @Mock
    private SeatReservationRepository seatReservationRepository;
    @Mock
    private BookingPaymentEventRepository paymentEventRepository;
    @Mock
    private ShowtimeClient showtimeClient;

    @Mock
    private com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager bookingMetricsManager;

    private BookingMapper bookingMapper = new BookingMapper();
    private BookingStatusTransitionService statusTransitionService = new BookingStatusTransitionService();

    private AdminBookingServiceImpl adminBookingService;

    private Booking sampleBooking;

    @BeforeEach
    public void setUp() {
        adminBookingService = new AdminBookingServiceImpl(
                bookingRepository,
                bookingMapper,
                statusTransitionService,
                historyService,
                auditService,
                operationLogService,
                outboxService,
                ticketService,
                snapshotService,
                bookingMetricsManager
        );

        sampleBooking = new Booking();
        sampleBooking.setId(10L);
        sampleBooking.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        sampleBooking.setBookingCode("BK1001");
        sampleBooking.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(sampleBooking, "bookingStatus", BookingStatus.PENDING_PAYMENT);
    }

    @Test
    public void findBookings_Success() {
        BookingFilterRequest filter = new BookingFilterRequest();
        filter.setPage(0);
        filter.setSize(10);

        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleBooking)));

        PagedResponse<BookingAdminResponse> result = adminBookingService.findBookings(filter);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    public void findBookings_UsesPersistedSnapshotForOperationalLabels() {
        BookingFilterRequest filter = new BookingFilterRequest();
        filter.setPage(0);
        filter.setSize(10);
        BookingSnapshotDto snapshot = new BookingSnapshotDto();
        snapshot.setMovieTitle("Nhà Có Năm Nàng Tiên");
        snapshot.setMoviePoster("https://cdn.example/poster.jpg");
        snapshot.setCinemaName("LoraFilm Hải Châu");
        snapshot.setAuditoriumName("4DX 01");
        snapshot.setShowtimeStart(java.time.Instant.parse("2026-07-27T12:30:00Z"));
        snapshot.setSeatCount(2);

        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleBooking)));
        when(snapshotService.findByBooking(10L)).thenReturn(snapshot);

        BookingAdminResponse result = adminBookingService.findBookings(filter).getContent().get(0);

        assertEquals("Nhà Có Năm Nàng Tiên", result.getMovieTitle());
        assertEquals("LoraFilm Hải Châu", result.getCinemaName());
        assertEquals("4DX 01", result.getAuditoriumName());
        assertEquals(2, result.getSeatCount());
    }

    @Test
    public void getBookingDetail_Success() {
        when(bookingRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(sampleBooking));
        when(ticketService.findByBooking(10L)).thenReturn(Collections.emptyList());
        when(historyService.findByBooking(10L)).thenReturn(Collections.emptyList());

        BookingDetailResponse result = adminBookingService.getBookingDetail("550e8400-e29b-41d4-a716-446655440000");

        assertNotNull(result);
        assertEquals("BK1001", result.getBookingCode());
        assertEquals(Collections.emptyList(), result.getTickets());
        assertEquals(Collections.emptyList(), result.getStatusHistories());
    }

    @Test
    public void getBookingDetail_EnrichesLegacySnapshotPresentationFacts() {
        sampleBooking.setShowtimePublicId("showtime-1");
        BookingSnapshotDto snapshot = new BookingSnapshotDto();
        snapshot.setShowtimeStart(java.time.Instant.parse("2026-08-10T02:00:00Z"));
        snapshot.setShowtimeEnd(java.time.Instant.parse("2026-08-10T04:30:00Z"));
        when(bookingRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(sampleBooking));
        when(snapshotService.findByBooking(10L)).thenReturn(snapshot);
        when(ticketService.findByBooking(10L)).thenReturn(Collections.emptyList());
        when(historyService.findByBooking(10L)).thenReturn(Collections.emptyList());
        when(showtimeClient.getPresentationByPublicId("showtime-1"))
                .thenReturn(new ShowtimePresentationContext("Movie 1", null, 150, "P"));
        ReflectionTestUtils.setField(adminBookingService, "showtimeClient", showtimeClient);

        BookingDetailResponse result = adminBookingService.getBookingDetail(
                "550e8400-e29b-41d4-a716-446655440000");

        assertEquals(150, result.getSnapshot().getDuration());
        assertEquals("P", result.getSnapshot().getAgeRating());
    }

    @Test
    public void getBookingDetail_UsesDatabaseReservationAndPaymentEventFacts() {
        SeatReservation reservation = new SeatReservation();
        reservation.setPublicId("reservation-public-id");
        reservation.setSeatPublicId("seat-public-id");
        reservation.setSeatLabel("B8");
        reservation.setSeatType("STANDARD");
        reservation.setStatus(SeatReservationStatus.HELD);
        reservation.setReservedAt(java.time.Instant.now());
        reservation.setExpiresAt(java.time.Instant.now().plusSeconds(600));
        AdminBookingServiceImpl serviceWithOperationalSources =
                new AdminBookingServiceImpl(
                        bookingRepository,
                        bookingMapper,
                        statusTransitionService,
                        historyService,
                        auditService,
                        operationLogService,
                        outboxService,
                        ticketService,
                        snapshotService,
                        bookingMetricsManager,
                        null,
                        null,
                        seatReservationRepository,
                        paymentEventRepository);
        when(bookingRepository.findByPublicId(
                "550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(sampleBooking));
        when(ticketService.findByBooking(10L)).thenReturn(Collections.emptyList());
        when(historyService.findByBooking(10L)).thenReturn(Collections.emptyList());
        when(seatReservationRepository.findAllByBookingId(10L))
                .thenReturn(List.of(reservation));
        when(paymentEventRepository.existsByBookingId(10L)).thenReturn(true);

        BookingDetailResponse result = serviceWithOperationalSources.getBookingDetail(
                "550e8400-e29b-41d4-a716-446655440000");

        assertEquals(SeatReservationStatus.HELD,
                result.getReservations().get(0).status());
        assertEquals("HELD", result.getOperationalInfo().reservationState());
        assertEquals(1, result.getOperationalInfo().heldSeatCount());
        assertEquals(true, result.getOperationalInfo().paymentAttempted());
    }

    @Test
    public void getOperationsSummary_UsesGlobalRepositoryCounts() {
        when(bookingRepository.count(any(Specification.class)))
                .thenReturn(20L, 5L, 4L, 3L, 2L, 1L, 0L, 2L, 1L, 1L, 3L);

        BookingOperationsSummaryResponse response =
                adminBookingService.getOperationsSummary();

        assertEquals(20L, response.totalBookings());
        assertEquals(5L, response.pendingPayment());
        assertEquals(3L, response.needsAttention());
    }

    @Test
    public void updateBookingStatus_Confirmed_IsPaymentTombstone() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Payment done", "ADMIN", "Note");

        when(bookingRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(sampleBooking));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminBookingService.updateBookingStatus("550e8400-e29b-41d4-a716-446655440000", request));

        assertEquals("CONFIRM_VIA_PAYMENT_RESULT_REQUIRED", exception.getErrorCode());
        verify(bookingRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    public void updateBookingStatus_Refunded_IsPaymentTombstone() {
        ReflectionTestUtils.setField(sampleBooking, "bookingStatus", BookingStatus.CONFIRMED);
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(
                BookingStatus.REFUNDED, "Admin requested refund", "ADMIN", null);
        when(bookingRepository.findByPublicId(
                "550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(sampleBooking));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminBookingService.updateBookingStatus(
                        "550e8400-e29b-41d4-a716-446655440000", request));

        assertEquals("REFUND_VIA_PAYMENT_RESULT_REQUIRED", exception.getErrorCode());
        verify(bookingRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    public void updateBookingStatus_PendingToCancelled_IsAllowedAdminCommand() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(
                BookingStatus.CANCELLED, "Khách yêu cầu hủy", "UNTRUSTED_SOURCE", null);
        when(bookingRepository.findByPublicId(
                "550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingAdminResponse response = adminBookingService.updateBookingStatus(
                "550e8400-e29b-41d4-a716-446655440000", request);

        assertEquals(BookingStatus.CANCELLED, response.getBookingStatus());
        verify(historyService).saveHistory(
                any(Booking.class),
                eq(BookingStatus.PENDING_PAYMENT.name()),
                eq(BookingStatus.CANCELLED.name()),
                eq("Khách yêu cầu hủy"),
                eq("ADMIN"),
                eq("ADMIN"));
    }

    @Test
    public void updateBookingStatus_ConfirmedToCompleted_IsAllowedAdminCommand() {
        ReflectionTestUtils.setField(sampleBooking, "bookingStatus", BookingStatus.CONFIRMED);
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(
                BookingStatus.COMPLETED, "Suất chiếu hoàn tất", "ADMIN", null);
        when(bookingRepository.findByPublicId(
                "550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingAdminResponse response = adminBookingService.updateBookingStatus(
                "550e8400-e29b-41d4-a716-446655440000", request);

        assertEquals(BookingStatus.COMPLETED, response.getBookingStatus());
    }

    @Test
    public void updateBookingStatus_InvalidTransition_ThrowsException() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Retry", "ADMIN", null);
        ReflectionTestUtils.setField(sampleBooking, "bookingStatus", BookingStatus.CANCELLED);

        when(bookingRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(sampleBooking));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminBookingService.updateBookingStatus("550e8400-e29b-41d4-a716-446655440000", request));
        assertEquals("CONFIRM_VIA_PAYMENT_RESULT_REQUIRED", ex.getErrorCode());
    }

    @Test
    public void updateBookingStatus_BookingNotFound_ThrowsException() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Reason", "ADMIN", null);
        when(bookingRepository.findByPublicId("99000000-0000-0000-0000-000000000000")).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () ->
                adminBookingService.updateBookingStatus("99000000-0000-0000-0000-000000000000", request));
    }
}

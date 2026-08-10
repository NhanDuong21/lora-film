package com.lorafilm.booking.booking.service.impl;

import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingTicketRepository;
import com.lorafilm.booking.booking.service.BookingLifecycleService;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.BookingStatusTransitionService;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalBookingEmergencyClosureTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingMapper bookingMapper;
    @Mock BookingStatusTransitionService statusTransitionService;
    @Mock BookingStatusHistoryService historyService;
    @Mock BookingAuditService auditService;
    @Mock BookingOperationLogService operationLogService;
    @Mock BookingOutboxService outboxService;
    @Mock BookingMetricsManager metricsManager;
    @Mock BookingTicketService ticketService;
    @Mock SeatReservationService reservationService;
    @Mock BookingLifecycleService lifecycleService;
    @Mock BookingTicketRepository ticketRepository;
    @Mock SeatReservationRepository reservationRepository;

    private InternalBookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InternalBookingServiceImpl(
                bookingRepository, bookingMapper, statusTransitionService, historyService,
                auditService, operationLogService, outboxService, metricsManager,
                ticketService, reservationService, lifecycleService,
                ticketRepository, reservationRepository);
    }

    @Test
    void releasesUnlinkedHoldsCancelsPendingAndKeepsPaidBookingForHandoff() {
        SeatReservation unlinked = reservation(90L, null, "A1");
        Booking pending = booking(1L, "pending-id", "BK-PENDING");
        Booking paid = booking(2L, "paid-id", "BK-PAID");
        paid.changeStatus(BookingStatus.CONFIRMED, Instant.now());
        paid.setPaymentStatus(PaymentStatus.SUCCESS);
        SeatReservation paidSeat = reservation(91L, 2L, "F5");

        when(reservationRepository.findUnlinkedHeldByShowtimePublicIdForUpdate("showtime-01"))
                .thenReturn(List.of(unlinked));
        when(bookingRepository.findByShowtimePublicIdForEmergencyUpdate("showtime-01"))
                .thenReturn(List.of(pending, paid));
        when(ticketRepository.findByBookingIdOrderBySeatLabelAsc(any())).thenReturn(List.of());
        when(reservationRepository.findAllByBookingId(1L)).thenReturn(List.of());
        when(reservationRepository.findAllByBookingId(2L)).thenReturn(List.of(paidSeat));
        when(lifecycleService.cancel(eq(pending), eq("EMERGENCY_AUDITORIUM_CLOSURE"),
                eq("Máy chiếu hỏng"), eq("MOVIE_SERVICE"))).thenReturn(pending);

        var result = service.closeShowtimeForEmergency("showtime-01", "Máy chiếu hỏng");

        assertThat(result.releasedUnlinkedSeatCount()).isEqualTo(1);
        assertThat(result.cancelledPendingBookingPublicIds()).containsExactly("pending-id");
        assertThat(result.paidBookings()).singleElement().satisfies(item -> {
            assertThat(item.bookingCode()).isEqualTo("BK-PAID");
            assertThat(item.seatLabels()).containsExactly("F5");
        });
        verify(reservationService).releaseSeatsInternal(
                List.of(90L), "Giải phóng do phòng chiếu đóng khẩn cấp: Máy chiếu hỏng");
    }

    private Booking booking(Long id, String publicId, String code) {
        Booking booking = Booking.create(
                publicId, code, 10L, 20L, 30L, 40L, 50L,
                new BigDecimal("180000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "VND", Instant.now().plusSeconds(900), null);
        booking.setId(id);
        booking.setShowtimePublicId("showtime-01");
        return booking;
    }

    private SeatReservation reservation(Long id, Long bookingId, String seatLabel) {
        SeatReservation reservation = new SeatReservation();
        reservation.setId(id);
        reservation.setBookingId(bookingId);
        reservation.setSeatLabel(seatLabel);
        return reservation;
    }
}

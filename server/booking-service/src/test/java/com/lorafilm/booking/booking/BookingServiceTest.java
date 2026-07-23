package com.lorafilm.booking.booking;

import com.lorafilm.booking.booking.client.ShowtimeBookingContext;
import com.lorafilm.booking.booking.client.ShowtimeClient;
import com.lorafilm.booking.booking.dto.request.CancelBookingRequest;
import com.lorafilm.booking.booking.dto.request.CreateBookingRequest;
import com.lorafilm.booking.booking.dto.response.BookingResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.impl.BookingServiceImpl;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.util.BookingCodeGenerator;
import com.lorafilm.booking.food.service.FoodOrderService;
import com.lorafilm.booking.reservation.dto.ConvertReservationRequest;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import com.lorafilm.booking.security.service.SecurityContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final String RESERVATION_PUBLIC_ID_1 = "8712253d-dc49-4f85-a6db-f99908dd61d7";
    private static final String RESERVATION_PUBLIC_ID_2 = "6f5867c6-9596-4011-844e-183f23e65bb6";
    private static final String SHOWTIME_PUBLIC_ID = "550e8400-e29b-41d4-a716-446655440001";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SeatReservationRepository reservationRepository;
    @Mock
    private SeatReservationService reservationService;
    @Mock
    private ShowtimeClient showtimeClient;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private BookingCodeGenerator bookingCodeGenerator;
    @Mock
    private FoodOrderService foodOrderService;
    @Mock
    private com.lorafilm.booking.payment.port.PaymentIntegrationPort paymentIntegrationPort;
    @Mock
    private com.lorafilm.booking.payment.repository.BookingPaymentEventRepository paymentEventRepository;
    @Mock
    private com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService;
    @Mock
    private com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager bookingMetricsManager;

    @Spy
    private BookingMapper bookingMapper = new BookingMapper();

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository,
                reservationRepository,
                reservationService,
                showtimeClient,
                securityContextService,
                bookingCodeGenerator,
                bookingMapper,
                foodOrderService,
                paymentIntegrationPort,
                paymentEventRepository,
                outboxService,
                bookingMetricsManager);
    }


    void shouldCreateBookingFromValidReservations() {
        Instant now = Instant.now();
        List<SeatReservation> reservations = List.of(
                reservation(21L, RESERVATION_PUBLIC_ID_1, 101L, 15L, 1001L, now.plusSeconds(300)),
                reservation(22L, RESERVATION_PUBLIC_ID_2, 102L, 15L, 1001L, now.plusSeconds(300)));
        CreateBookingRequest request = new CreateBookingRequest(
                SHOWTIME_PUBLIC_ID,
                List.of(RESERVATION_PUBLIC_ID_1, RESERVATION_PUBLIC_ID_2));

        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(reservationRepository.findAllByPublicIdInForUpdate(request.getReservationPublicIds()))
                .thenReturn(reservations);
        when(showtimeClient.getBookingContext(1001L, List.of(101L, 102L)))
                .thenReturn(showtimeContext(now));
        when(bookingCodeGenerator.generate()).thenReturn("LORAFILM-20260720-000001");
        when(bookingRepository.existsByBookingCode("LORAFILM-20260720-000001")).thenReturn(false);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(100L);
            booking.setPublicId("550e8400-e29b-41d4-a716-446655440000");
            booking.setCreatedAt(now);
            return booking;
        });

        BookingResponse response = bookingService.createBooking(request);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.publicId());
        assertEquals(BookingStatus.PENDING_PAYMENT, response.status());
        assertEquals(new BigDecimal("240000.00"), response.totalAmount());
        ArgumentCaptor<ConvertReservationRequest> captor = ArgumentCaptor.forClass(ConvertReservationRequest.class);
        verify(reservationService).convertReservations(captor.capture());
        assertEquals(100L, captor.getValue().getBookingId());
        assertEquals(List.of(21L, 22L), captor.getValue().getReservationIds());
    }

    @Test
    void shouldRejectReservationOwnedByAnotherUser() {
        Instant now = Instant.now();
        CreateBookingRequest request = new CreateBookingRequest(
                SHOWTIME_PUBLIC_ID, List.of(RESERVATION_PUBLIC_ID_1));
        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(reservationRepository.findAllByPublicIdInForUpdate(request.getReservationPublicIds()))
                .thenReturn(List.of(reservation(
                        21L, RESERVATION_PUBLIC_ID_1, 101L, 99L, 1001L, now.plusSeconds(300))));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> bookingService.createBooking(request));

        assertEquals("BOOKING_RESERVATION_OWNER_MISMATCH", exception.getErrorCode());
        verify(showtimeClient, never()).getBookingContext(any(), any());
    }

    @Test
    void shouldCancelOwnedPendingBooking() {
        Booking booking = existingBooking(Instant.now().plusSeconds(900));
        booking.setId(100L);
        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(bookingRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking(
                "550e8400-e29b-41d4-a716-446655440000",
                new CancelBookingRequest("USER_CANCEL", "Changed plans"));

        assertEquals(BookingStatus.CANCELLED, response.status());
        assertEquals("USER_CANCEL", booking.getCancelReasonCode());
        assertEquals("Changed plans", booking.getCancelReasonDetail());
    }

    private SeatReservation reservation(
            Long id, String publicId, Long seatId, Long userId, Long showtimeId, Instant expiresAt) {
        SeatReservation reservation = new SeatReservation();
        reservation.setId(id);
        reservation.setPublicId(publicId);
        reservation.setSeatId(seatId);
        reservation.setUserId(userId);
        reservation.setShowtimeId(showtimeId);
        reservation.setStatus(SeatReservationStatus.HELD);
        reservation.setExpiresAt(expiresAt);
        return reservation;
    }

    private ShowtimeBookingContext showtimeContext(Instant now) {
        return new ShowtimeBookingContext(
                1001L,
                SHOWTIME_PUBLIC_ID,
                101L,
                201L,
                301L,
                "OPEN_FOR_BOOKING",
                now.plusSeconds(1800),
                now.plusSeconds(9000),
                now.plusSeconds(900),
                new BigDecimal("240000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("240000"),
                "VND",
                "Superman",
                "Lora Cinema",
                "Room 1",
                List.of(
                        new ShowtimeBookingContext.SeatContext(101L, "A01", "STANDARD", new BigDecimal("120000")),
                        new ShowtimeBookingContext.SeatContext(102L, "A02", "STANDARD", new BigDecimal("120000"))));
    }

    private Booking existingBooking(Instant expiresAt) {
        return Booking.create(
                "550e8400-e29b-41d4-a716-446655440000",
                "LORAFILM-20260720-000001",
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
                expiresAt,
                null);
    }
}

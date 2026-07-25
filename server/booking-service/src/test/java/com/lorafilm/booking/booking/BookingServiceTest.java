package com.lorafilm.booking.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.client.ShowtimeBookingContext;
import com.lorafilm.booking.booking.client.ShowtimeClient;
import com.lorafilm.booking.booking.dto.request.CancelBookingRequest;
import com.lorafilm.booking.booking.dto.request.CreateBookingRequest;
import com.lorafilm.booking.booking.dto.response.BookingResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import com.lorafilm.booking.booking.dto.BookingPriceSnapshotPayload;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingPriceSnapshotRepository;
import com.lorafilm.booking.booking.service.impl.BookingServiceImpl;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.util.BookingCodeGenerator;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private BookingPriceSnapshotRepository priceSnapshotRepository;
    @Mock
    private com.lorafilm.booking.booking.repository.BookingSnapshotRepository bookingSnapshotRepository;
    @Mock
    private com.lorafilm.booking.booking.repository.BookingTicketRepository bookingTicketRepository;
    @Mock
    private com.lorafilm.booking.payment.port.PaymentIntegrationPort paymentIntegrationPort;
    @Mock
    private com.lorafilm.booking.payment.repository.BookingPaymentEventRepository paymentEventRepository;
    @Mock
    private com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService;
    @Mock
    private com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager bookingMetricsManager;
    @Mock
    private com.lorafilm.booking.booking.service.BookingTicketService bookingTicketService;
    @Mock
    private com.lorafilm.booking.booking.service.BookingSnapshotService bookingSnapshotService;

        @Spy
        private BookingMapper bookingMapper = new BookingMapper();

    private BookingServiceImpl bookingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        bookingService = new BookingServiceImpl(
                bookingRepository,
                reservationRepository,
                reservationService,
                showtimeClient,
                securityContextService,
                bookingCodeGenerator,
                bookingMapper,
                priceSnapshotRepository,
                objectMapper,
                bookingSnapshotRepository,
                bookingTicketRepository,
                paymentIntegrationPort,
                paymentEventRepository,
                outboxService,
                bookingMetricsManager,
                bookingTicketService,
                bookingSnapshotService);
    }

        @Test
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
        when(priceSnapshotRepository.existsByBookingId(100L)).thenReturn(false);

                BookingResponse response = bookingService.createBooking(request);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.publicId());
        assertEquals(BookingStatus.PENDING_PAYMENT, response.status());
        assertEquals(new BigDecimal("240000.00"), response.totalAmount());
        ArgumentCaptor<ConvertReservationRequest> captor = ArgumentCaptor.forClass(ConvertReservationRequest.class);
        verify(reservationService).convertReservations(captor.capture());
        assertEquals(100L, captor.getValue().getBookingId());
        assertEquals(List.of(21L, 22L), captor.getValue().getReservationIds());
        verify(showtimeClient).getBookingContext(1001L, List.of(101L, 102L));
        ArgumentCaptor<BookingPriceSnapshot> snapshotCaptor =
                ArgumentCaptor.forClass(BookingPriceSnapshot.class);
        verify(priceSnapshotRepository).save(snapshotCaptor.capture());
        BookingPriceSnapshot snapshot = snapshotCaptor.getValue();
        assertEquals("VND", snapshot.getCurrency());
        assertEquals("showtime-snapshot-v1", snapshot.getPricingEngineVersion());
        try {
            BookingPriceSnapshotPayload payload = objectMapper.readValue(
                    snapshot.getPricingBreakdownJson(), BookingPriceSnapshotPayload.class);
            assertEquals(new BigDecimal("240000"), payload.authoritativeTicketTotal());
            assertEquals(List.of(101L, 102L), payload.seats().stream()
                    .map(BookingPriceSnapshotPayload.SeatPriceLine::seatId).toList());
            assertEquals(new BigDecimal("240000"), payload.seats().stream()
                    .map(BookingPriceSnapshotPayload.SeatPriceLine::unitPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new AssertionError(exception);
        }
        verify(bookingSnapshotService).createSnapshot(eq(100L), any());
        verify(bookingMetricsManager).incrementBookingCreated();
    }

    @Test
    void shouldRejectMixedCurrencySeatLinesBeforePersistingBooking() {
        Instant now = Instant.now();
        List<SeatReservation> reservations = List.of(
                reservation(21L, RESERVATION_PUBLIC_ID_1, 101L, 15L, 1001L, now.plusSeconds(300)),
                reservation(22L, RESERVATION_PUBLIC_ID_2, 102L, 15L, 1001L, now.plusSeconds(300)));
        CreateBookingRequest request = new CreateBookingRequest(
                SHOWTIME_PUBLIC_ID, List.of(RESERVATION_PUBLIC_ID_1, RESERVATION_PUBLIC_ID_2));
        ShowtimeBookingContext context = showtimeContext(now);
        context = new ShowtimeBookingContext(
                context.showtimeId(), context.showtimePublicId(), context.movieId(), context.cinemaId(),
                context.auditoriumId(), context.status(), context.startsAt(), context.endsAt(),
                context.paymentExpiresAt(), context.ticketAmount(), context.serviceFee(),
                context.discountAmount(), context.totalAmount(), context.currency(), context.movieTitle(),
                context.cinemaName(), context.auditoriumName(),
                List.of(
                        context.seats().get(0),
                        new ShowtimeBookingContext.SeatContext(
                                102L, "A02", "STANDARD", new BigDecimal("120000"), "USD")));
        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(reservationRepository.findAllByPublicIdInForUpdate(request.getReservationPublicIds()))
                .thenReturn(reservations);
        when(showtimeClient.getBookingContext(1001L, List.of(101L, 102L))).thenReturn(context);

        assertThrows(com.lorafilm.booking.common.exception.IntegrationException.class,
                () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).saveAndFlush(any());
        verify(priceSnapshotRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateAuthoritativeSnapshotPersistence() {
        Instant now = Instant.now();
        List<SeatReservation> reservations = List.of(
                reservation(21L, RESERVATION_PUBLIC_ID_1, 101L, 15L, 1001L, now.plusSeconds(300)),
                reservation(22L, RESERVATION_PUBLIC_ID_2, 102L, 15L, 1001L, now.plusSeconds(300)));
        CreateBookingRequest request = new CreateBookingRequest(
                SHOWTIME_PUBLIC_ID, List.of(RESERVATION_PUBLIC_ID_1, RESERVATION_PUBLIC_ID_2));
        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(reservationRepository.findAllByPublicIdInForUpdate(request.getReservationPublicIds()))
                .thenReturn(reservations);
        when(showtimeClient.getBookingContext(1001L, List.of(101L, 102L))).thenReturn(showtimeContext(now));
        when(bookingCodeGenerator.generate()).thenReturn("LORAFILM-20260720-000001");
        when(bookingRepository.existsByBookingCode("LORAFILM-20260720-000001")).thenReturn(false);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(100L);
            return booking;
        });
        when(priceSnapshotRepository.existsByBookingId(100L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.createBooking(request));

        assertEquals("BOOKING_PRICE_SNAPSHOT_EXISTS", exception.getErrorCode());
        verify(priceSnapshotRepository, never()).save(any());
        verify(reservationService, never()).convertReservations(any());
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
                        new ShowtimeBookingContext.SeatContext(101L, "A01", "STANDARD", new BigDecimal("120000"), "VND"),
                        new ShowtimeBookingContext.SeatContext(102L, "A02", "STANDARD", new BigDecimal("120000"), "VND")));
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

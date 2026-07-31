package com.lorafilm.booking.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.client.ShowtimeBookingContext;
import com.lorafilm.booking.booking.client.ShowtimeClient;
import com.lorafilm.booking.booking.client.ScoreRedemptionClient;
import com.lorafilm.booking.booking.dto.request.CancelBookingRequest;
import com.lorafilm.booking.booking.dto.request.CreateBookingRequest;
import com.lorafilm.booking.booking.dto.request.FinalizeCheckoutRequest;
import com.lorafilm.booking.booking.dto.response.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.response.BookingResponse;
import com.lorafilm.booking.booking.dto.response.BookingSpendingSummaryResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import com.lorafilm.booking.booking.entity.BookingSnapshot;
import com.lorafilm.booking.booking.dto.BookingPriceSnapshotPayload;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingPriceSnapshotRepository;
import com.lorafilm.booking.booking.service.impl.BookingServiceImpl;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.util.BookingCodeGenerator;
import com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

        private static final String RESERVATION_PUBLIC_ID_1 = "8712253d-dc49-4f85-a6db-f99908dd61d7";
        private static final String RESERVATION_PUBLIC_ID_2 = "6f5867c6-9596-4011-844e-183f23e65bb6";
        private static final String SHOWTIME_PUBLIC_ID = "550e8400-e29b-41d4-a716-446655440001";
        private static final String COUPLE_SEAT_PUBLIC_ID = "550e8400-e29b-41d4-a716-446655440091";
        private static final String SEAT_PUBLIC_ID_A2 = "550e8400-e29b-41d4-a716-446655440102";
        private static final String SEAT_PUBLIC_ID_A3 = "550e8400-e29b-41d4-a716-446655440103";

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
    @Mock
    private ScoreRedemptionClient scoreRedemptionClient;

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
        bookingService.setScoreRedemptionClient(scoreRedemptionClient);
        lenient().when(showtimeClient.getSeatLayout(anyLong()))
                .thenReturn(nonAdjacentDefaultSeatLayout());
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
        verify(reservationService, never()).convertReservations(any());
        assertEquals(100L, reservations.get(0).getBookingId());
        assertEquals(SeatReservationStatus.HELD, reservations.get(0).getStatus());
        assertEquals(100L, reservations.get(1).getBookingId());
        assertEquals(SeatReservationStatus.HELD, reservations.get(1).getStatus());
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
                context.moviePosterUrl(), context.cinemaName(), context.auditoriumName(),
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
    void shouldRejectSingleCoupleSeatBeforePersistingBooking() {
        Instant now = Instant.now();
        CreateBookingRequest request = new CreateBookingRequest(
                SHOWTIME_PUBLIC_ID, List.of(COUPLE_SEAT_PUBLIC_ID), true);

        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(showtimeClient.getBookingContextByPublicId(
                SHOWTIME_PUBLIC_ID, List.of(COUPLE_SEAT_PUBLIC_ID)))
                .thenReturn(singleCoupleSeatShowtimeContext(now));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.createBooking(request));

        assertEquals("SEAT_COUPLE_PAIR_REQUIRED", exception.getErrorCode());
        verify(showtimeClient).getBookingContextByPublicId(
                SHOWTIME_PUBLIC_ID, List.of(COUPLE_SEAT_PUBLIC_ID));
        verify(bookingRepository, never()).saveAndFlush(any());
        verify(priceSnapshotRepository, never()).save(any());
    }

    @Test
    void shouldRejectSelectionThatLeavesSingleSeatGapBeforePersistingBooking() {
        Instant now = Instant.now();
        CreateBookingRequest request = new CreateBookingRequest(
                SHOWTIME_PUBLIC_ID, List.of(SEAT_PUBLIC_ID_A2, SEAT_PUBLIC_ID_A3), true);

        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(showtimeClient.getBookingContextByPublicId(
                SHOWTIME_PUBLIC_ID, List.of(SEAT_PUBLIC_ID_A2, SEAT_PUBLIC_ID_A3)))
                .thenReturn(singleGapShowtimeContext(now));
        when(showtimeClient.getSeatLayout(1001L)).thenReturn(adjacentThreeSeatLayout());

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.createBooking(request));

        assertEquals("SEAT_SINGLE_GAP_NOT_ALLOWED", exception.getErrorCode());
        verify(bookingRepository, never()).saveAndFlush(any());
        verify(priceSnapshotRepository, never()).save(any());
    }

    @Test
    void shouldRejectSecondActiveBookingForSameCustomerAndShowtime() {
        Instant now = Instant.now();
        CreateBookingRequest request = new CreateBookingRequest(
                SHOWTIME_PUBLIC_ID, List.of(RESERVATION_PUBLIC_ID_1));
        SeatReservation requestedReservation = reservation(
                21L, RESERVATION_PUBLIC_ID_1, 101L, 15L, 1001L, now.plusSeconds(300));
        Booking activeBooking = existingBooking(now.plusSeconds(600));

        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(reservationRepository.findAllByPublicIdInForUpdate(request.getReservationPublicIds()))
                .thenReturn(List.of(requestedReservation));
        when(showtimeClient.getBookingContext(1001L, List.of(101L)))
                .thenReturn(singleSeatShowtimeContext(now));
        when(bookingRepository.findPendingByUserAndShowtimeForUpdate(
                15L, 1001L, BookingStatus.PENDING_PAYMENT))
                .thenReturn(List.of(activeBooking));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.createBooking(request));

        assertEquals("BOOKING_ACTIVE_SHOWTIME_EXISTS", exception.getErrorCode());
        verify(bookingRepository, never()).saveAndFlush(any());
        verify(priceSnapshotRepository, never()).save(any());
    }

    @Test
    void shouldExpireStaleBookingBeforeCreatingReplacementForSameShowtime() {
        Instant now = Instant.now();
        CreateBookingRequest request = new CreateBookingRequest(
                SHOWTIME_PUBLIC_ID, List.of(RESERVATION_PUBLIC_ID_1));
        SeatReservation requestedReservation = reservation(
                21L, RESERVATION_PUBLIC_ID_1, 101L, 15L, 1001L, now.plusSeconds(300));
        Booking staleBooking = existingBooking(now.minusSeconds(1));
        staleBooking.setId(91L);

        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(reservationRepository.findAllByPublicIdInForUpdate(request.getReservationPublicIds()))
                .thenReturn(List.of(requestedReservation));
        when(showtimeClient.getBookingContext(1001L, List.of(101L)))
                .thenReturn(singleSeatShowtimeContext(now));
        when(bookingRepository.findPendingByUserAndShowtimeForUpdate(
                15L, 1001L, BookingStatus.PENDING_PAYMENT))
                .thenReturn(List.of(staleBooking));
        when(bookingCodeGenerator.generate()).thenReturn("LORAFILM-20260720-000002");
        when(bookingRepository.existsByBookingCode("LORAFILM-20260720-000002")).thenReturn(false);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking replacement = invocation.getArgument(0);
            replacement.setId(100L);
            replacement.setCreatedAt(now);
            return replacement;
        });
        when(priceSnapshotRepository.existsByBookingId(100L)).thenReturn(false);

        BookingResponse response = bookingService.createBooking(request);

        assertEquals(BookingStatus.EXPIRED, staleBooking.getBookingStatus());
        assertEquals(BookingStatus.PENDING_PAYMENT, response.status());
        verify(reservationService).handleBookingStatusChange(
                91L,
                BookingStatus.EXPIRED,
                "Expired before creating a replacement Booking for the same showtime");
        verify(bookingRepository).flush();
    }

    @Test
    void shouldTranslateDatabaseUniqueCollisionIntoActiveBookingConflict() {
        Instant now = Instant.now();
        CreateBookingRequest request = new CreateBookingRequest(
                SHOWTIME_PUBLIC_ID, List.of(RESERVATION_PUBLIC_ID_1));

        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(reservationRepository.findAllByPublicIdInForUpdate(request.getReservationPublicIds()))
                .thenReturn(List.of(reservation(
                        21L, RESERVATION_PUBLIC_ID_1, 101L, 15L, 1001L, now.plusSeconds(300))));
        when(showtimeClient.getBookingContext(1001L, List.of(101L)))
                .thenReturn(singleSeatShowtimeContext(now));
        when(bookingCodeGenerator.generate()).thenReturn("LORAFILM-20260720-000003");
        when(bookingRepository.existsByBookingCode("LORAFILM-20260720-000003")).thenReturn(false);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenThrow(
                new DataIntegrityViolationException(
                        "Duplicate entry for key 'uk_active_customer_showtime_booking'"));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.createBooking(request));

        assertEquals("BOOKING_ACTIVE_SHOWTIME_EXISTS", exception.getErrorCode());
        verify(priceSnapshotRepository, never()).save(any());
    }

    @Test
    void shouldReturnOnlyCurrentCustomersActiveBookingForShowtime() {
        Instant now = Instant.now();
        Booking activeBooking = existingBooking(now.plusSeconds(600));
        activeBooking.setShowtimePublicId(SHOWTIME_PUBLIC_ID);

        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(bookingRepository.findActiveByUserAndShowtimePublicId(
                eq(15L),
                eq(SHOWTIME_PUBLIC_ID),
                eq(BookingStatus.PENDING_PAYMENT),
                any(Instant.class)))
                .thenReturn(List.of(activeBooking));

        Optional<BookingResponse> response = bookingService.findActiveByShowtime(SHOWTIME_PUBLIC_ID);

        assertEquals(activeBooking.getPublicId(), response.orElseThrow().publicId());
        verify(bookingRepository).findActiveByUserAndShowtimePublicId(
                eq(15L),
                eq(SHOWTIME_PUBLIC_ID),
                eq(BookingStatus.PENDING_PAYMENT),
                any(Instant.class));
    }

    @Test
    void shouldReturnAnnualSpendingFromSuccessfulPaidBookings() {
        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(bookingRepository.sumPaidSpendingByUserAndPeriod(
                eq(15L),
                eq(List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED)),
                eq(PaymentStatus.SUCCESS),
                any(Instant.class),
                any(Instant.class)))
                .thenReturn(new BigDecimal("176000.00"));

        BookingSpendingSummaryResponse response =
                bookingService.getMySpendingSummary(2026);

        assertEquals(2026, response.year());
        assertEquals(new BigDecimal("176000.00"), response.totalSpending());
        assertEquals("VND", response.currency());
        assertEquals(
                Instant.parse("2025-12-31T17:00:00Z"),
                response.periodStart());
        assertEquals(
                Instant.parse("2026-12-31T17:00:00Z"),
                response.periodEnd());
    }

    @Test
    void shouldReturnZeroWhenCustomerHasNoPaidSpending() {
        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(bookingRepository.sumPaidSpendingByUserAndPeriod(
                eq(15L),
                any(),
                eq(PaymentStatus.SUCCESS),
                any(Instant.class),
                any(Instant.class)))
                .thenReturn(null);

        BookingSpendingSummaryResponse response =
                bookingService.getMySpendingSummary(2026);

        assertEquals(new BigDecimal("0.00"), response.totalSpending());
    }

    @Test
    void shouldReturnCustomerPresentationFromImmutableSnapshots() throws Exception {
        Instant now = Instant.now();
        Booking booking = existingBooking(now.plusSeconds(900));
        booking.setId(100L);
        booking.setCreatedAt(now);

        BookingSnapshot displaySnapshot = new BookingSnapshot();
        displaySnapshot.setMovieTitle("Superman");
        displaySnapshot.setMoviePoster("https://cdn.lorafilm.test/superman-poster.jpg");
        displaySnapshot.setCinemaName("Lora Cinema");
        displaySnapshot.setAuditoriumName("Phòng 1");
        displaySnapshot.setShowtimeStart(now.plusSeconds(1800));

        ShowtimeBookingContext context = showtimeContext(now);
        BookingPriceSnapshotPayload pricePayload = new BookingPriceSnapshotPayload(
                context.showtimeId(),
                context.showtimePublicId(),
                now,
                context.currency(),
                context.movieId(),
                context.movieTitle(),
                context.ticketAmount(),
                context.seats().stream()
                        .map(seat -> new BookingPriceSnapshotPayload.SeatPriceLine(
                                seat.seatId(), seat.seatLabel(), seat.seatType(),
                                seat.price(), seat.seatPublicId()))
                        .toList());
        BookingPriceSnapshot priceSnapshot = new BookingPriceSnapshot();
        priceSnapshot.setPricingBreakdownJson(objectMapper.writeValueAsString(pricePayload));

        when(securityContextService.getCurrentUserId()).thenReturn(15L);
        when(bookingRepository.findByPublicId(booking.getPublicId())).thenReturn(Optional.of(booking));
        when(bookingSnapshotRepository.findByBookingId(100L)).thenReturn(Optional.of(displaySnapshot));
        when(priceSnapshotRepository.findByBookingId(100L)).thenReturn(Optional.of(priceSnapshot));

        BookingDetailResponse response = bookingService.findById(booking.getPublicId());

        assertEquals("Superman", response.presentation().movieTitle());
        assertEquals("https://cdn.lorafilm.test/superman-poster.jpg",
                response.presentation().moviePosterUrl());
        assertEquals(List.of("A01", "A02"), response.presentation().seats().stream()
                .map(seat -> seat.label())
                .toList());
        assertEquals(new BigDecimal("240000"), response.ticketAmount());
        assertEquals(BigDecimal.ZERO, response.food().totalAmount());
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

        @Test
        void shouldHoldScoreAndLockTheDiscountedCheckoutAmount() {
                Booking booking = existingBooking(Instant.now().plusSeconds(900));
                booking.setId(100L);
                when(securityContextService.getCurrentUserId()).thenReturn(15L);
                when(bookingRepository.findByPublicIdWithLock(booking.getPublicId()))
                                .thenReturn(Optional.of(booking));
                when(scoreRedemptionClient.hold(
                                eq(15L),
                                eq(100L),
                                eq(50),
                                any(Integer.class),
                                eq(new BigDecimal("240000.00")),
                                any(String.class),
                                eq("score-idem-1")))
                                .thenReturn(new ScoreRedemptionClient.ScoreHoldResult(
                                                "HOLD-1",
                                                50,
                                                "ACTIVE",
                                                new BigDecimal("50000.00"),
                                                new BigDecimal("1000"),
                                                false));
                when(bookingRepository.saveAndFlush(booking)).thenReturn(booking);

                BookingResponse response = bookingService.finalizeCheckout(
                                booking.getPublicId(),
                                new FinalizeCheckoutRequest(50, "score-idem-1"));

                assertEquals(50, response.scorePointsUsed());
                assertEquals(new BigDecimal("50000.00"), response.scoreDiscount());
                assertEquals(new BigDecimal("190000.00"), response.totalAmount());
                verify(scoreRedemptionClient).hold(
                                eq(15L),
                                eq(100L),
                                eq(50),
                                any(Integer.class),
                                eq(new BigDecimal("240000.00")),
                                any(String.class),
                                eq("score-idem-1"));
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
                "https://cdn.lorafilm.test/superman-poster.jpg",
                "Lora Cinema",
                "Room 1",
                List.of(
                        new ShowtimeBookingContext.SeatContext(101L, "A01", "STANDARD", new BigDecimal("120000"), "VND"),
                        new ShowtimeBookingContext.SeatContext(102L, "A02", "STANDARD", new BigDecimal("120000"), "VND")));
    }

    private ShowtimeBookingContext singleSeatShowtimeContext(Instant now) {
        ShowtimeBookingContext fullContext = showtimeContext(now);
        return new ShowtimeBookingContext(
                fullContext.showtimeId(),
                fullContext.showtimePublicId(),
                fullContext.movieId(),
                fullContext.cinemaId(),
                fullContext.auditoriumId(),
                fullContext.status(),
                fullContext.startsAt(),
                fullContext.endsAt(),
                fullContext.paymentExpiresAt(),
                new BigDecimal("120000"),
                fullContext.serviceFee(),
                fullContext.discountAmount(),
                new BigDecimal("120000"),
                fullContext.currency(),
                fullContext.movieTitle(),
                fullContext.moviePosterUrl(),
                fullContext.cinemaName(),
                fullContext.auditoriumName(),
                List.of(fullContext.seats().getFirst()));
    }

    private ShowtimeBookingContext singleCoupleSeatShowtimeContext(Instant now) {
        ShowtimeBookingContext fullContext = showtimeContext(now);
        return new ShowtimeBookingContext(
                fullContext.showtimeId(),
                fullContext.showtimePublicId(),
                fullContext.movieId(),
                fullContext.cinemaId(),
                fullContext.auditoriumId(),
                fullContext.status(),
                fullContext.startsAt(),
                fullContext.endsAt(),
                fullContext.paymentExpiresAt(),
                new BigDecimal("78000"),
                fullContext.serviceFee(),
                fullContext.discountAmount(),
                new BigDecimal("78000"),
                fullContext.currency(),
                fullContext.movieTitle(),
                fullContext.moviePosterUrl(),
                fullContext.cinemaName(),
                fullContext.auditoriumName(),
                List.of(new ShowtimeBookingContext.SeatContext(
                        101L,
                        COUPLE_SEAT_PUBLIC_ID,
                        "I1",
                        "COUPLE",
                        new BigDecimal("78000"),
                        "VND",
                        "I-01")));
    }

    private ShowtimeBookingContext singleGapShowtimeContext(Instant now) {
        ShowtimeBookingContext fullContext = showtimeContext(now);
        return new ShowtimeBookingContext(
                fullContext.showtimeId(),
                fullContext.showtimePublicId(),
                fullContext.movieId(),
                fullContext.cinemaId(),
                fullContext.auditoriumId(),
                fullContext.status(),
                fullContext.startsAt(),
                fullContext.endsAt(),
                fullContext.paymentExpiresAt(),
                new BigDecimal("240000"),
                fullContext.serviceFee(),
                fullContext.discountAmount(),
                new BigDecimal("240000"),
                fullContext.currency(),
                fullContext.movieTitle(),
                fullContext.moviePosterUrl(),
                fullContext.cinemaName(),
                fullContext.auditoriumName(),
                List.of(
                        new ShowtimeBookingContext.SeatContext(
                                102L, SEAT_PUBLIC_ID_A2, "A2", "STANDARD",
                                new BigDecimal("120000"), "VND", null),
                        new ShowtimeBookingContext.SeatContext(
                                103L, SEAT_PUBLIC_ID_A3, "A3", "STANDARD",
                                new BigDecimal("120000"), "VND", null)));
    }

    private ShowtimeSeatLayoutResponse nonAdjacentDefaultSeatLayout() {
        return seatLayout(List.of(
                layoutSeat(101L, "A1", "STANDARD", 1, 1, null),
                layoutSeat(102L, "B1", "STANDARD", 2, 1, null)));
    }

    private ShowtimeSeatLayoutResponse adjacentThreeSeatLayout() {
        return seatLayout(List.of(
                layoutSeat(101L, "A1", "STANDARD", 1, 1, null),
                layoutSeat(102L, "A2", "STANDARD", 1, 2, null),
                layoutSeat(103L, "A3", "STANDARD", 1, 3, null)));
    }

    private ShowtimeSeatLayoutResponse seatLayout(
            List<ShowtimeSeatLayoutResponse.SeatDetailDto> seats) {
        return new ShowtimeSeatLayoutResponse(
                1001L,
                Instant.now().plusSeconds(1800),
                Instant.now().plusSeconds(9000),
                "OPEN_FOR_BOOKING",
                301L,
                seats);
    }

    private ShowtimeSeatLayoutResponse.SeatDetailDto layoutSeat(
            Long id,
            String code,
            String type,
            int row,
            int column,
            String pairGroup) {
        ShowtimeSeatLayoutResponse.SeatDetailDto seat =
                new ShowtimeSeatLayoutResponse.SeatDetailDto(
                        id, code, type, null, false, row, column);
        seat.setStatus("ACTIVE");
        seat.setPairGroup(pairGroup);
        return seat;
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

package com.lorafilm.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.audit.repository.BookingAuditLogRepository;
import com.lorafilm.booking.audit.repository.BookingOperationLogRepository;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.common.exception.SeatReservationException;
import com.lorafilm.booking.config.BookingPolicyProperties;
import com.lorafilm.booking.config.ReservationProperties;
import com.lorafilm.booking.infrastructure.client.MovieServiceClient;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.infrastructure.repository.BookingOutboxEventRepository;
import com.lorafilm.booking.reservation.dto.HoldSeatRequest;
import com.lorafilm.booking.reservation.dto.PublicSeatAvailabilityResponse;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.mapper.SeatReservationMapper;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.service.RedisLockService;
import com.lorafilm.booking.reservation.service.impl.SeatReservationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BookingReservationAuthorityTest {

    @Mock
    private SeatReservationRepository reservationRepository;
    @Mock
    private BookingAuditLogRepository auditRepository;
    @Mock
    private BookingOperationLogRepository operationRepository;
    @Mock
    private BookingOutboxEventRepository outboxRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private RedisLockService redisLockService;
    @Mock
    private MovieServiceClient movieServiceClient;
    @Mock
    private BookingMetricsManager metricsManager;

    private SeatReservationServiceImpl service;

    @BeforeEach
    void setUp() {
        BookingPolicyProperties policy = new BookingPolicyProperties();
        policy.setMaxSeatsPerBooking(8);
        policy.setHoldDurationSeconds(900);
        policy.setCreationLockTtlSeconds(30);
        service = new SeatReservationServiceImpl(
                reservationRepository,
                auditRepository,
                operationRepository,
                outboxRepository,
                bookingRepository,
                redisLockService,
                new ReservationProperties(900),
                policy,
                new SeatReservationMapper(),
                new ObjectMapper().findAndRegisterModules(),
                movieServiceClient,
                metricsManager);
        var layout = new com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse(
                77L,
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                "OPEN",
                1L,
                List.of(
                        new com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse.SeatDetailDto(
                                2L, "A2", "STANDARD", null, false, 1, 2),
                        new com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse.SeatDetailDto(
                                4L, "A4", "STANDARD", null, false, 1, 4)));
        lenient().when(movieServiceClient.getShowtimeSeatLayout(anyLong())).thenReturn(layout);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void releasesOnlyOwnedShortLockAfterCommit() {
        when(redisLockService.acquireHoldLocks(eq(77L), anyList(), anyString(), eq(30L)))
                .thenReturn(true);
        when(reservationRepository.findReservationsForBookingUpdate(eq(77L), anyList()))
                .thenReturn(List.of());
        when(reservationRepository.findSoldSeatIdsFromBookings(eq(77L), anyList()))
                .thenReturn(List.of());
        when(reservationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        service.holdSeats(11L, new HoldSeatRequest(77L, List.of(4L, 2L)));

        verify(redisLockService, never()).releaseLocks(anyLong(), anyList(), anyString());
        TransactionSynchronization synchronization =
                TransactionSynchronizationManager.getSynchronizations().getFirst();
        synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        ArgumentCaptor<String> acquiredToken = ArgumentCaptor.forClass(String.class);
        verify(redisLockService).acquireHoldLocks(
                eq(77L), eq(List.of(4L, 2L)), acquiredToken.capture(), eq(30L));
        verify(redisLockService).releaseLocks(
                eq(77L), eq(List.of(4L, 2L)), eq(acquiredToken.getValue()));
    }

    @Test
    void releasesOnlyOwnedShortLockAfterRollback() {
        when(redisLockService.acquireHoldLocks(eq(77L), anyList(), anyString(), eq(30L)))
                .thenReturn(true);
        SeatReservation held = reservation(4L, "seat-4", SeatReservationStatus.HELD,
                Instant.now().plusSeconds(60));
        when(reservationRepository.findReservationsForBookingUpdate(eq(77L), anyList()))
                .thenReturn(List.of(held));

        TransactionSynchronizationManager.initSynchronization();
        assertThrows(SeatReservationException.class,
                () -> service.holdSeats(11L, new HoldSeatRequest(77L, List.of(4L))));

        verify(redisLockService, never()).releaseLocks(anyLong(), anyList(), anyString());
        TransactionSynchronizationManager.getSynchronizations().getFirst()
                .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(redisLockService).releaseLocks(eq(77L), eq(List.of(4L)), anyString());
    }

    @Test
    void availabilityUsesDatabaseHeldAndBookedRowsAndNeverRedis() {
        SeatReservation held = reservation(4L, "seat-4", SeatReservationStatus.HELD,
                Instant.now().plusSeconds(60));
        SeatReservation booked = reservation(5L, "seat-5", SeatReservationStatus.BOOKED,
                Instant.now().minusSeconds(60));
        when(reservationRepository.findAllActiveReservationsByShowtimePublicId(
                eq("showtime-public"), any())).thenReturn(List.of(held, booked));

        PublicSeatAvailabilityResponse response =
                service.checkPublicAvailability("showtime-public");

        assertEquals(2, response.occupiedSeats().size());
        assertTrue(response.occupiedSeats().stream()
                .anyMatch(seat -> seat.seatPublicId().equals("seat-4")
                        && seat.status().equals("HELD")));
        assertTrue(response.occupiedSeats().stream()
                .anyMatch(seat -> seat.seatPublicId().equals("seat-5")
                        && seat.status().equals("BOOKED")));
        verifyNoInteractions(redisLockService);
    }

    @Test
    void releasedOrExpiredDatabaseRowsAreReusable() {
        when(reservationRepository.findAllActiveReservationsByShowtimePublicId(
                eq("showtime-public"), any())).thenReturn(List.of());

        PublicSeatAvailabilityResponse response =
                service.checkPublicAvailability("showtime-public");

        assertTrue(response.occupiedSeats().isEmpty());
        verifyNoInteractions(redisLockService);
    }

    private SeatReservation reservation(Long seatId, String seatPublicId,
                                        SeatReservationStatus status, Instant expiresAt) {
        SeatReservation reservation = new SeatReservation();
        reservation.setSeatId(seatId);
        reservation.setSeatPublicId(seatPublicId);
        reservation.setShowtimeId(77L);
        reservation.setShowtimePublicId("showtime-public");
        reservation.setStatus(status);
        reservation.setExpiresAt(expiresAt);
        return reservation;
    }
}

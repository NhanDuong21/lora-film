package com.lorafilm.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.audit.repository.BookingAuditLogRepository;
import com.lorafilm.booking.audit.repository.BookingOperationLogRepository;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.common.exception.SeatReservationException;
import com.lorafilm.booking.config.ReservationProperties;
import com.lorafilm.booking.infrastructure.repository.BookingOutboxEventRepository;
import com.lorafilm.booking.reservation.dto.ConvertReservationRequest;
import com.lorafilm.booking.reservation.dto.HoldSeatRequest;
import com.lorafilm.booking.reservation.dto.HoldSeatResponse;
import com.lorafilm.booking.reservation.dto.ReleaseSeatRequest;
import com.lorafilm.booking.reservation.dto.SeatAvailabilityResponse;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.mapper.SeatReservationMapper;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.service.RedisLockService;
import com.lorafilm.booking.reservation.service.impl.SeatReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SeatReservationServiceTest {

    @Mock
    private SeatReservationRepository seatReservationRepository;
    @Mock
    private BookingAuditLogRepository auditLogRepository;
    @Mock
    private BookingOperationLogRepository operationLogRepository;
    @Mock
    private BookingOutboxEventRepository outboxEventRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private RedisLockService redisLockService;
    @Mock
    private ReservationProperties reservationProperties;
    @Mock
    private SeatReservationMapper seatReservationMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private com.lorafilm.booking.infrastructure.client.MovieServiceClient movieServiceClient;

    @InjectMocks
    private SeatReservationServiceImpl seatReservationService;

    @BeforeEach
    public void setUp() {
        lenient().when(reservationProperties.getReservationTimeout()).thenReturn(300L);
    }

    @Test
    public void holdSeats_Success() {
        Long userId = 100L;
        HoldSeatRequest request = new HoldSeatRequest(1001L, List.of(15L, 16L));

        when(redisLockService.acquireHoldLocks(eq(1001L), anyList(), anyString(), anyLong())).thenReturn(true);
        when(seatReservationRepository.findActiveReservations(eq(1001L), anyList(), any())).thenReturn(List.of());
        when(seatReservationRepository.findSoldSeatIdsFromBookings(eq(1001L), anyList())).thenReturn(List.of());

        SeatReservation res1 = new SeatReservation();
        res1.setId(101L);
        res1.setShowtimeId(1001L);
        res1.setSeatId(15L);
        res1.setUserId(userId);
        res1.setStatus(SeatReservationStatus.HELD);

        SeatReservation res2 = new SeatReservation();
        res2.setId(102L);
        res2.setShowtimeId(1001L);
        res2.setSeatId(16L);
        res2.setUserId(userId);
        res2.setStatus(SeatReservationStatus.HELD);

        when(seatReservationRepository.saveAll(anyList())).thenReturn(List.of(res1, res2));

        HoldSeatResponse response = seatReservationService.holdSeats(userId, request);

        assertNotNull(response);
        assertEquals(2, response.getReservationIds().size());
        verify(redisLockService).acquireHoldLocks(eq(1001L), anyList(), anyString(), eq(300L));
        verify(outboxEventRepository, times(2)).save(any());
    }

    @Test
    public void holdSeats_LockFailure_ThrowsException() {
        Long userId = 100L;
        HoldSeatRequest request = new HoldSeatRequest(1001L, List.of(15L));

        when(redisLockService.acquireHoldLocks(eq(1001L), anyList(), anyString(), anyLong())).thenReturn(false);

        SeatReservationException ex = assertThrows(SeatReservationException.class, () ->
                seatReservationService.holdSeats(userId, request));

        assertEquals("SEAT_009", ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    public void holdSeats_SeatAlreadyHeld_RollsBackLock() {
        Long userId = 100L;
        HoldSeatRequest request = new HoldSeatRequest(1001L, List.of(15L));

        when(redisLockService.acquireHoldLocks(eq(1001L), anyList(), anyString(), anyLong())).thenReturn(true);

        SeatReservation existing = new SeatReservation();
        existing.setSeatId(15L);
        when(seatReservationRepository.findActiveReservations(eq(1001L), anyList(), any())).thenReturn(List.of(existing));

        SeatReservationException ex = assertThrows(SeatReservationException.class, () ->
                seatReservationService.holdSeats(userId, request));

        assertEquals("SEAT_003", ex.getErrorCode());
        verify(redisLockService).releaseLocks(eq(1001L), anyList(), anyString());
    }

    @Test
    public void releaseSeats_Success() {
        Long userId = 100L;
        ReleaseSeatRequest request = new ReleaseSeatRequest(List.of(101L), "User cancel");

        SeatReservation res = new SeatReservation();
        res.setId(101L);
        res.setUserId(userId);
        res.setShowtimeId(1001L);
        res.setSeatId(15L);
        res.setStatus(SeatReservationStatus.HELD);

        when(seatReservationRepository.findAllByIdIn(List.of(101L))).thenReturn(List.of(res));

        seatReservationService.releaseSeats(userId, request);

        assertEquals(SeatReservationStatus.RELEASED, res.getStatus());
        verify(redisLockService).releaseLocks(eq(1001L), anyList(), eq("*"));
        verify(outboxEventRepository).save(any());
    }

    @Test
    public void releaseSeats_WrongUser_ThrowsForbidden() {
        Long userId = 100L;
        ReleaseSeatRequest request = new ReleaseSeatRequest(List.of(101L));

        SeatReservation res = new SeatReservation();
        res.setId(101L);
        res.setUserId(999L); // Different user
        res.setStatus(SeatReservationStatus.HELD);

        when(seatReservationRepository.findAllByIdIn(List.of(101L))).thenReturn(List.of(res));

        SeatReservationException ex = assertThrows(SeatReservationException.class, () ->
                seatReservationService.releaseSeats(userId, request));

        assertEquals("SEAT_008", ex.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    public void convertReservations_Success() {
        ConvertReservationRequest request = new ConvertReservationRequest(50L, List.of(101L));

        SeatReservation res = new SeatReservation();
        res.setId(101L);
        res.setShowtimeId(1001L);
        res.setSeatId(15L);
        res.setStatus(SeatReservationStatus.HELD);
        res.setExpiresAt(Instant.now().plusSeconds(100));

        when(seatReservationRepository.findAllByIdIn(List.of(101L))).thenReturn(List.of(res));

        seatReservationService.convertReservations(request);

        assertEquals(SeatReservationStatus.BOOKED, res.getStatus());
        verify(redisLockService).releaseLocks(eq(1001L), anyList(), eq("*"));
        verify(outboxEventRepository).save(any());
    }

    @Test
    public void convertReservations_Expired_ThrowsException() {
        ConvertReservationRequest request = new ConvertReservationRequest(50L, List.of(101L));

        SeatReservation res = new SeatReservation();
        res.setId(101L);
        res.setStatus(SeatReservationStatus.EXPIRED);
        res.setExpiresAt(Instant.now().minusSeconds(100));

        when(seatReservationRepository.findAllByIdIn(List.of(101L))).thenReturn(List.of(res));

        SeatReservationException ex = assertThrows(SeatReservationException.class, () ->
                seatReservationService.convertReservations(request));

        assertEquals("SEAT_006", ex.getErrorCode());
    }

    @Test
    public void checkAvailability_ReturnsOccupiedSeats() {
        SeatReservation activeRes = new SeatReservation();
        activeRes.setSeatId(15L);

        when(seatReservationRepository.findActiveReservations(eq(1001L), anyList(), any())).thenReturn(List.of(activeRes));
        when(seatReservationRepository.findSoldSeatIdsFromBookings(eq(1001L), anyList())).thenReturn(List.of(16L));

        SeatAvailabilityResponse response = seatReservationService.checkAvailability(1001L, List.of(15L, 16L, 17L));

        assertFalse(response.isAvailable());
        assertEquals(2, response.getUnavailableSeats().size());
        assertTrue(response.getUnavailableSeats().contains(15L));
        assertTrue(response.getUnavailableSeats().contains(16L));
    }

    @Test
    public void getOccupiedSeatsByShowtime_Success() {
        Long showtimeId = 1001L;
        SeatReservation activeRes = new SeatReservation();
        activeRes.setSeatId(15L);
        activeRes.setSeatLabel("A1");
        activeRes.setStatus(SeatReservationStatus.HELD);
        activeRes.setExpiresAt(Instant.now().plusSeconds(300));

        when(seatReservationRepository.findAllActiveReservationsByShowtimeId(eq(showtimeId), any())).thenReturn(List.of(activeRes));
        when(seatReservationRepository.findSoldSeatIdsFromBookingsByShowtimeId(eq(showtimeId))).thenReturn(List.of(16L));

        com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse response = seatReservationService.getOccupiedSeatsByShowtime("1001");

        assertNotNull(response);
        assertEquals(2, response.getTotalOccupied());
        assertEquals(2, response.getOccupiedSeats().size());
    }

    @Test
    public void getOccupiedSeatsByShowtime_WithPublicId_Success() {
        String showtimePublicId = "46d15faf-84ca-11f1-89f5-22158c0adccb";
        Long showtimeId = 1001L;

        com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse layout = new com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse();
        layout.setShowtimeId(showtimeId);
        layout.setShowtimePublicId(showtimePublicId);

        when(movieServiceClient.getShowtimeSeatLayoutByPublicId(showtimePublicId)).thenReturn(layout);
        when(seatReservationRepository.findAllActiveReservationsByShowtimeId(eq(showtimeId), any())).thenReturn(List.of());
        when(seatReservationRepository.findSoldSeatIdsFromBookingsByShowtimeId(eq(showtimeId))).thenReturn(List.of());

        com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse response = seatReservationService.getOccupiedSeatsByShowtime(showtimePublicId);

        assertNotNull(response);
        assertEquals(showtimePublicId, response.getShowtimeIdentifier());
        assertEquals(0, response.getTotalOccupied());
    }

    @Test
    public void extendReservation_Success() {
        String publicId = "pub-123";
        Long userId = 100L;
        Instant now = Instant.now();

        SeatReservation res = new SeatReservation();
        res.setPublicId(publicId);
        res.setReservationCode("RES-12345");
        res.setUserId(userId);
        res.setShowtimeId(1001L);
        res.setSeatId(15L);
        res.setStatus(SeatReservationStatus.HELD);
        res.setReservedAt(now.minusSeconds(100));
        res.setExpiresAt(now.plusSeconds(200));

        when(seatReservationRepository.findByPublicId(publicId)).thenReturn(Optional.of(res));
        when(seatReservationRepository.save(any())).thenReturn(res);

        com.lorafilm.booking.reservation.dto.ExtendReservationResponse response = seatReservationService.extendReservation(publicId, userId);

        assertNotNull(response);
        assertEquals(publicId, response.getPublicId());
        assertEquals(180L, response.getExtendedSeconds());
        verify(redisLockService).extendLockTtl(eq(1001L), eq(15L), eq(publicId), anyLong());
    }

    @Test
    public void handleBookingStatusChange_Cancelled_ReleasesSeats() {
        Long bookingId = 50L;
        SeatReservation res = new SeatReservation();
        res.setId(101L);
        res.setBookingId(bookingId);
        res.setStatus(SeatReservationStatus.BOOKED);

        when(seatReservationRepository.findAllByBookingId(bookingId)).thenReturn(List.of(res));

        seatReservationService.handleBookingStatusChange(bookingId, com.lorafilm.booking.booking.enums.BookingStatus.CANCELLED, "User cancelled");

        assertEquals(SeatReservationStatus.RELEASED, res.getStatus());
        assertEquals("User cancelled", res.getExpiredReason());
        verify(seatReservationRepository).save(res);
        verify(outboxEventRepository).save(any());
    }
}

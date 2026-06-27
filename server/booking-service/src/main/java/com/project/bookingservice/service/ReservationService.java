package com.project.bookingservice.service;

import com.project.bookingservice.config.BookingProperties;
import com.project.bookingservice.dto.movie.SeatInfo;
import com.project.bookingservice.dto.movie.ShowtimeInfo;
import com.project.bookingservice.dto.reservation.CreateReservationRequest;
import com.project.bookingservice.dto.reservation.ReservationGroupResponse;
import com.project.bookingservice.dto.reservation.ReservationResponse;
import com.project.bookingservice.dto.reservation.ReservationSeatResponse;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.exception.BusinessException;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.security.CurrentUserProvider;
import com.project.bookingservice.service.idempotency.IdempotencyService;
import com.project.bookingservice.service.lock.SeatLockManager;
import com.project.bookingservice.service.movie.MovieServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private final SeatReservationRepository seatReservationRepository;
    private final MovieServiceClient movieServiceClient;
    private final SeatLockManager seatLockManager;
    private final IdempotencyService idempotencyService;
    private final CurrentUserProvider currentUserProvider;
    private final BookingProperties bookingProperties;

    public ReservationService(SeatReservationRepository seatReservationRepository,
            MovieServiceClient movieServiceClient,
            SeatLockManager seatLockManager,
            IdempotencyService idempotencyService,
            CurrentUserProvider currentUserProvider,
            BookingProperties bookingProperties) {
        this.seatReservationRepository = seatReservationRepository;
        this.movieServiceClient = movieServiceClient;
        this.seatLockManager = seatLockManager;
        this.idempotencyService = idempotencyService;
        this.currentUserProvider = currentUserProvider;
        this.bookingProperties = bookingProperties;
    }

    @Transactional
    public ReservationGroupResponse createReservation(CreateReservationRequest request, String idempotencyKey) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "seatIds cannot be empty");
        }

        if (request.getSeatIds().size() != new HashSet<>(request.getSeatIds()).size()) {
            throw new BusinessException("VALIDATION_ERROR", "seatIds contains duplicates");
        }

        Long userId = currentUserProvider.getCurrentUserId();

        // 1. Idempotency Check
        if (idempotencyService.hasKey(userId, idempotencyKey)) {
            ReservationGroupResponse previousResponse = idempotencyService.getResponse(userId, idempotencyKey, request);
            if (previousResponse != null) {
                logger.info("Idempotency replay for key {}", idempotencyKey);
                return previousResponse;
            } else {
                logger.warn("Idempotency conflict for key {}", idempotencyKey);
                throw new BusinessException("BOOKING_IDEMPOTENCY_CONFLICT");
            }
        }

        Long showtimeId = request.getShowtimeId();

        // 2. Validate Showtime
        ShowtimeInfo showtime = movieServiceClient.getShowtime(showtimeId);
        if (showtime == null) {
            throw new BusinessException("BOOKING_SHOWTIME_NOT_FOUND");
        }
        if (!showtime.isAvailable()) {
            throw new BusinessException("BOOKING_SHOWTIME_NOT_AVAILABLE");
        }

        // 3. Validate Seats
        List<SeatInfo> seats = movieServiceClient.getSeats(request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw new BusinessException("BOOKING_SEAT_NOT_FOUND");
        }

        for (SeatInfo seat : seats) {
            if (!seat.isActive()) {
                throw new BusinessException("BOOKING_SEAT_NOT_ACTIVE");
            }
            if (!seat.getRoomId().equals(showtime.getRoomId())) {
                throw new BusinessException("BOOKING_SEAT_ROOM_MISMATCH");
            }
            if (movieServiceClient.isSeatBooked(showtimeId, seat.getId())) {
                throw new BusinessException("BOOKING_SEAT_ALREADY_BOOKED");
            }
        }

        // 4. Check DB for existing HELD reservations
        List<SeatReservation> existingReservations = seatReservationRepository.findActiveReservations(
                showtimeId, request.getSeatIds());
        if (!existingReservations.isEmpty()) {
            throw new BusinessException("BOOKING_SEAT_ALREADY_HELD");
        }

        // 5. Acquire Redis Locks atomically
        boolean locksAcquired = seatLockManager.acquireLocks(showtimeId, request.getSeatIds(), idempotencyKey);
        if (!locksAcquired) {
            logger.warn("Failed to acquire Redis locks for showtimeId: {}, seatIds: {}", showtimeId,
                    request.getSeatIds());
            throw new BusinessException("BOOKING_SEAT_ALREADY_HELD");
        }

        try {
            // 6. Create reservations
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusMinutes(bookingProperties.getReservation().getTtlSeconds() / 60);

            List<SeatReservation> newReservations = new ArrayList<>();
            for (Long seatId : request.getSeatIds()) {
                SeatReservation reservation = new SeatReservation(showtimeId, seatId, userId, expiresAt);
                // JPA will set created_at and version
                newReservations.add(reservation);
            }

            List<SeatReservation> savedReservations = seatReservationRepository.saveAll(newReservations);

            List<ReservationSeatResponse> seatResponses = savedReservations.stream()
                    .map(r -> new ReservationSeatResponse(r.getId(), r.getSeatId()))
                    .collect(Collectors.toList());
                    
            ReservationGroupResponse groupResponse = new ReservationGroupResponse(
                    showtimeId,
                    userId,
                    ReservationStatus.HELD,
                    expiresAt,
                    seatResponses
            );

            // 7. Save idempotency result
            idempotencyService.saveResponse(userId, idempotencyKey, request, groupResponse);
            logger.info("Reservation created successfully for idempotencyKey: {}", idempotencyKey);
            return groupResponse;
        } catch (Exception e) {
            // Rollback locks on failure
            seatLockManager.releaseLocks(showtimeId, request.getSeatIds(), idempotencyKey);
            logger.error("Error creating reservation, releasing locks for showtimeId: {}, seatIds: {}", showtimeId,
                    request.getSeatIds(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long reservationId) {
        SeatReservation reservation = seatReservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("SEAT_RESERVATION_NOT_FOUND"));

        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (!reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException("FORBIDDEN");
        }

        return new ReservationResponse(reservation.getId(), reservation.getUserId(), reservation.getShowtimeId(), reservation.getSeatId(),
                reservation.getStatus(), reservation.getExpiresAt(), reservation.getCreatedAt());
    }

    @Transactional
    public void releaseReservation(Long reservationId) {
        SeatReservation reservation = seatReservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("SEAT_RESERVATION_NOT_FOUND"));

        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (!reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException("FORBIDDEN");
        }

        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return; // Idempotent success
        }

        if (reservation.getStatus() == ReservationStatus.CONVERTED) {
            throw new BusinessException("SEAT_RESERVATION_ALREADY_CONVERTED");
        }

        if (LocalDateTime.now().isAfter(reservation.getExpiresAt())) {
            throw new BusinessException("SEAT_RESERVATION_EXPIRED");
        }

        // Release lock
        seatLockManager.forceReleaseLocks(reservation.getShowtimeId(), List.of(reservation.getSeatId()));
        logger.info("Released redis lock for showtime {}, seat {}", reservation.getShowtimeId(),
                reservation.getSeatId());

        reservation.setStatus(ReservationStatus.RELEASED);
        seatReservationRepository.save(reservation);
        logger.info("Reservation {} released successfully", reservationId);
    }
}

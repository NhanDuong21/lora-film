package com.lorafilm.booking.reservation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.audit.entity.BookingAuditLog;
import com.lorafilm.booking.audit.entity.BookingOperationLog;
import com.lorafilm.booking.audit.repository.BookingAuditLogRepository;
import com.lorafilm.booking.audit.repository.BookingOperationLogRepository;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.common.exception.SeatReservationException;
import com.lorafilm.booking.config.ReservationProperties;
import com.lorafilm.booking.infrastructure.client.MovieServiceClient;
import com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse;
import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;
import com.lorafilm.booking.infrastructure.enums.OutboxStatus;
import com.lorafilm.booking.infrastructure.repository.BookingOutboxEventRepository;
import com.lorafilm.booking.reservation.dto.ConvertReservationRequest;
import com.lorafilm.booking.reservation.dto.HoldSeatRequest;
import com.lorafilm.booking.reservation.dto.HoldSeatResponse;
import com.lorafilm.booking.reservation.dto.ReleaseSeatRequest;
import com.lorafilm.booking.reservation.dto.SeatAvailabilityResponse;
import com.lorafilm.booking.reservation.dto.SeatReservationResponse;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.ReservationSource;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.mapper.SeatReservationMapper;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.service.RedisLockService;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SeatReservationServiceImpl implements SeatReservationService {

    private static final Logger log = LoggerFactory.getLogger(SeatReservationServiceImpl.class);

    private final SeatReservationRepository seatReservationRepository;
    private final BookingAuditLogRepository auditLogRepository;
    private final BookingOperationLogRepository operationLogRepository;
    private final BookingOutboxEventRepository outboxEventRepository;
    private final BookingRepository bookingRepository;
    private final RedisLockService redisLockService;
    private final ReservationProperties reservationProperties;
    private final SeatReservationMapper seatReservationMapper;
    private final ObjectMapper objectMapper;
    private final com.lorafilm.booking.infrastructure.client.MovieServiceClient movieServiceClient;

    public SeatReservationServiceImpl(
            SeatReservationRepository seatReservationRepository,
            BookingAuditLogRepository auditLogRepository,
            BookingOperationLogRepository operationLogRepository,
            BookingOutboxEventRepository outboxEventRepository,
            BookingRepository bookingRepository,
            RedisLockService redisLockService,
            ReservationProperties reservationProperties,
            SeatReservationMapper seatReservationMapper,
            ObjectMapper objectMapper,
            com.lorafilm.booking.infrastructure.client.MovieServiceClient movieServiceClient) {
        this.seatReservationRepository = seatReservationRepository;
        this.auditLogRepository = auditLogRepository;
        this.operationLogRepository = operationLogRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.bookingRepository = bookingRepository;
        this.redisLockService = redisLockService;
        this.reservationProperties = reservationProperties;
        this.seatReservationMapper = seatReservationMapper;
        this.objectMapper = objectMapper;
        this.movieServiceClient = movieServiceClient;
    }

    @Override
    public HoldSeatResponse holdSeats(Long userId, HoldSeatRequest request) {
        long startTime = System.currentTimeMillis();

        if (userId == null) {
            throw new SeatReservationException("SEAT_008", "User must be authenticated", HttpStatus.UNAUTHORIZED);
        }
        if (request == null || request.getShowtimeId() == null || request.getShowtimeId() <= 0) {
            throw new SeatReservationException("SEAT_002", "Invalid showtime ID", HttpStatus.BAD_REQUEST);
        }
        List<Long> seatIds = request.getSeatIds();
        if (seatIds == null || seatIds.isEmpty()) {
            throw new SeatReservationException("SEAT_001", "Seat list cannot be empty", HttpStatus.BAD_REQUEST);
        }
        if (seatIds.size() > 10) {
            throw new SeatReservationException("SEAT_001", "Cannot hold more than 10 seats per request", HttpStatus.BAD_REQUEST);
        }
        Set<Long> uniqueSeats = new HashSet<>(seatIds);
        if (uniqueSeats.size() < seatIds.size()) {
            throw new SeatReservationException("SEAT_001", "Duplicate seat IDs are not allowed in the request", HttpStatus.BAD_REQUEST);
        }

        Long showtimeId = request.getShowtimeId();

        // Bug Fix 4: Validate Max Held Seats Per User limit (e.g. max 10 active held seats per user for a showtime)
        long existingHeldCount = seatReservationRepository.countActiveHeldSeatsByUserAndShowtime(userId, showtimeId, Instant.now());
        if (existingHeldCount + seatIds.size() > 10) {
            throw new SeatReservationException("SEAT_001", "Cannot hold more than 10 seats per user for this showtime", HttpStatus.BAD_REQUEST);
        }

        String lockToken = UUID.randomUUID().toString();
        long ttlSeconds = reservationProperties.getReservationTimeout();
        // Bug Fix 1: Pass showtimeId to acquireHoldLocks to scope locks per showtime
        boolean acquired = redisLockService.acquireHoldLocks(showtimeId, seatIds, lockToken, ttlSeconds);
        if (!acquired) {
            throw new SeatReservationException("SEAT_009", "Failed to acquire Redis lock for one or more seats", HttpStatus.CONFLICT);
        }

        try {
            return executeHoldSeatsTransaction(userId, showtimeId, seatIds, lockToken, ttlSeconds, startTime);
        } catch (Exception ex) {
            redisLockService.releaseLocks(showtimeId, seatIds, lockToken);
            throw ex;
        }
    }

    @Transactional
    public HoldSeatResponse executeHoldSeatsTransaction(
            Long userId, Long showtimeId, List<Long> seatIds, String lockToken, long ttlSeconds, long startTime) {

        Instant now = Instant.now();

        // Bug Fix 2, 3 & 7: Validate Showtime & Seat Layout from MovieServiceClient if available
        com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse layout = movieServiceClient.getShowtimeSeatLayout(showtimeId);
        Map<Long, com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse.SeatDetailDto> seatMap = new java.util.HashMap<>();

        if (layout != null) {
            if ("CANCELLED".equalsIgnoreCase(layout.getStatus()) || "INACTIVE".equalsIgnoreCase(layout.getStatus())) {
                throw new SeatReservationException("SHOWTIME_001", "Showtime is cancelled or inactive", HttpStatus.BAD_REQUEST);
            }
            if (layout.getStartTime() != null && layout.getStartTime().isBefore(now)) {
                throw new SeatReservationException("SHOWTIME_002", "Cannot hold seats for a past showtime", HttpStatus.BAD_REQUEST);
            }
            if (layout.getSeats() != null) {
                for (com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse.SeatDetailDto seatDto : layout.getSeats()) {
                    seatMap.put(seatDto.getSeatId(), seatDto);
                }
            }

            // Validate requested seats
            for (Long seatId : seatIds) {
                com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse.SeatDetailDto detail = seatMap.get(seatId);
                if (detail != null) {
                    if (detail.isBlocked()) {
                        throw new SeatReservationException("SEAT_004", "Seat " + (detail.getSeatCode() != null ? detail.getSeatCode() : seatId) + " is blocked or out of order", HttpStatus.CONFLICT);
                    }
                    // Bug Fix 5: Couple Seat Pairing Rule
                    if ("COUPLE".equalsIgnoreCase(detail.getSeatType()) || detail.getPairedSeatId() != null) {
                        Long pairId = detail.getPairedSeatId();
                        if (pairId != null && !seatIds.contains(pairId)) {
                            throw new SeatReservationException("SEAT_COUPLE_PAIR_REQUIRED", "Couple seat " + (detail.getSeatCode() != null ? detail.getSeatCode() : seatId) + " must be reserved together with its pair seat", HttpStatus.BAD_REQUEST);
                        }
                    }
                }
            }
        }

        List<SeatReservation> activeReservations = seatReservationRepository.findActiveReservations(showtimeId, seatIds, now);
        if (!activeReservations.isEmpty()) {
            throw new SeatReservationException("SEAT_003", "One or more seats are already held by another user", HttpStatus.CONFLICT);
        }

        List<Long> soldSeatIds = seatReservationRepository.findSoldSeatIdsFromBookings(showtimeId, seatIds);
        if (!soldSeatIds.isEmpty()) {
            throw new SeatReservationException("SEAT_004", "One or more seats are already sold", HttpStatus.CONFLICT);
        }

        Instant expiresAt = now.plusSeconds(ttlSeconds);
        List<SeatReservation> reservationsToSave = new ArrayList<>();
        List<Long> reservationIds = new ArrayList<>();
        List<String> publicIds = new ArrayList<>();

        for (Long seatId : seatIds) {
            String pubId = UUID.randomUUID().toString();
            SeatReservation reservation = new SeatReservation();
            reservation.setPublicId(pubId);
            reservation.setReservationCode("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            reservation.setShowtimeId(showtimeId);
            reservation.setSeatId(seatId);

            // Bug Fix 7: Use actual seatCode and seatType from movie-service layout if available
            com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse.SeatDetailDto seatDetail = seatMap.get(seatId);
            String label = (seatDetail != null && seatDetail.getSeatCode() != null) ? seatDetail.getSeatCode() : ("SEAT-" + seatId);
            String sType = (seatDetail != null && seatDetail.getSeatType() != null) ? seatDetail.getSeatType() : "STANDARD";
            reservation.setSeatLabel(label);
            reservation.setSeatType(sType);

            reservation.setUserId(userId);
            reservation.setReservationSource(ReservationSource.WEB);
            reservation.setStatus(SeatReservationStatus.HELD);
            reservation.setExpiresAt(expiresAt);
            reservation.setReservedAt(now);

            reservationsToSave.add(reservation);
            publicIds.add(pubId);
        }

        List<SeatReservation> savedReservations = seatReservationRepository.saveAll(reservationsToSave);
        for (SeatReservation saved : savedReservations) {
            reservationIds.add(saved.getId());

            recordAuditLog(userId.toString(), "HOLD_SEAT", "status", null, saved.getStatus().name());
            recordOutboxEvent("SeatReservation", saved.getId(), "SEAT_RESERVED", saved);
        }

        recordOperationLog(null, "HOLD_SEATS", true, (int) (System.currentTimeMillis() - startTime), null, null);

        return new HoldSeatResponse(reservationIds, publicIds, expiresAt);
    }

    @Override
    @Transactional
    public void releaseSeats(Long userId, ReleaseSeatRequest request) {
        long startTime = System.currentTimeMillis();

        if (userId == null) {
            throw new SeatReservationException("SEAT_008", "User must be authenticated", HttpStatus.UNAUTHORIZED);
        }
        if (request == null || request.getReservationIds() == null || request.getReservationIds().isEmpty()) {
            throw new SeatReservationException("SEAT_005", "Reservation IDs cannot be empty", HttpStatus.BAD_REQUEST);
        }

        List<SeatReservation> reservations = seatReservationRepository.findAllByIdIn(request.getReservationIds());
        if (reservations.size() < request.getReservationIds().size()) {
            throw new SeatReservationException("SEAT_005", "One or more reservations do not exist", HttpStatus.NOT_FOUND);
        }

        String reason = request.getReason() != null ? request.getReason() : "Customer released reservation";

        for (SeatReservation reservation : reservations) {
            if (!reservation.getUserId().equals(userId)) {
                throw new SeatReservationException("SEAT_008", "Reservation does not belong to user", HttpStatus.FORBIDDEN);
            }
            if (reservation.getStatus() == SeatReservationStatus.BOOKED) {
                throw new SeatReservationException("SEAT_007", "Reservation is already converted to booking", HttpStatus.BAD_REQUEST);
            }
            if (reservation.getStatus() == SeatReservationStatus.EXPIRED) {
                throw new SeatReservationException("SEAT_006", "Reservation is already expired", HttpStatus.BAD_REQUEST);
            }
        }

        Map<Long, List<Long>> seatsByShowtime = new java.util.HashMap<>();

        for (SeatReservation reservation : reservations) {
            if (reservation.getStatus() == SeatReservationStatus.HELD) {
                reservation.setStatus(SeatReservationStatus.RELEASED);
                reservation.setExpiredReason(reason);
                seatReservationRepository.save(reservation);

                seatsByShowtime.computeIfAbsent(reservation.getShowtimeId(), k -> new ArrayList<>()).add(reservation.getSeatId());

                recordAuditLog(userId.toString(), "RELEASE_SEAT", "status", "HELD", "RELEASED");
                recordOutboxEvent("SeatReservation", reservation.getId(), "SEAT_RELEASED", reservation);
            }
        }

        for (Map.Entry<Long, List<Long>> entry : seatsByShowtime.entrySet()) {
            redisLockService.releaseLocks(entry.getKey(), entry.getValue(), "*");
        }

        recordOperationLog(null, "RELEASE_SEATS", true, (int) (System.currentTimeMillis() - startTime), null, null);
    }

    @Override
    @Transactional
    public void releaseSeatsInternal(List<Long> reservationIds, String reason) {
        long startTime = System.currentTimeMillis();

        if (reservationIds == null || reservationIds.isEmpty()) {
            return;
        }

        List<SeatReservation> reservations = seatReservationRepository.findAllByIdIn(reservationIds);
        Map<Long, List<Long>> seatsByShowtime = new java.util.HashMap<>();
        String releaseReason = reason != null ? reason : "Internal release request";

        for (SeatReservation reservation : reservations) {
            if (reservation.getStatus() == SeatReservationStatus.HELD) {
                reservation.setStatus(SeatReservationStatus.RELEASED);
                reservation.setExpiredReason(releaseReason);
                seatReservationRepository.save(reservation);

                seatsByShowtime.computeIfAbsent(reservation.getShowtimeId(), k -> new ArrayList<>()).add(reservation.getSeatId());

                recordAuditLog("SYSTEM", "RELEASE_SEAT", "status", "HELD", "RELEASED");
                recordOutboxEvent("SeatReservation", reservation.getId(), "SEAT_RELEASED", reservation);
            }
        }

        for (Map.Entry<Long, List<Long>> entry : seatsByShowtime.entrySet()) {
            redisLockService.releaseLocks(entry.getKey(), entry.getValue(), "*");
        }

        recordOperationLog(null, "RELEASE_SEATS_INTERNAL", true, (int) (System.currentTimeMillis() - startTime), null, null);
    }

    @Override
    @Transactional
    public void convertReservations(ConvertReservationRequest request) {
        long startTime = System.currentTimeMillis();

        if (request == null || request.getBookingId() == null) {
            throw new SeatReservationException("SEAT_005", "Booking ID is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getReservationIds() == null || request.getReservationIds().isEmpty()) {
            throw new SeatReservationException("SEAT_005", "Reservation IDs cannot be empty", HttpStatus.BAD_REQUEST);
        }

        List<SeatReservation> reservations = seatReservationRepository.findAllByIdIn(request.getReservationIds());
        if (reservations.size() < request.getReservationIds().size()) {
            throw new SeatReservationException("SEAT_005", "One or more reservations do not exist", HttpStatus.NOT_FOUND);
        }

        Instant now = Instant.now();
        Map<Long, List<Long>> seatsByShowtime = new java.util.HashMap<>();

        for (SeatReservation reservation : reservations) {
            if (reservation.getStatus() == SeatReservationStatus.BOOKED) {
                recordOperationLog(request.getBookingId(), "CONVERT_RESERVATION", false, (int) (System.currentTimeMillis() - startTime),
                        "SEAT_007", "Reservation is already converted to booking");
                throw new SeatReservationException("SEAT_007", "Reservation is already converted to booking", HttpStatus.BAD_REQUEST);
            }
            if (reservation.getStatus() == SeatReservationStatus.EXPIRED || reservation.getStatus() == SeatReservationStatus.RELEASED || reservation.getExpiresAt().isBefore(now)) {
                recordOperationLog(request.getBookingId(), "CONVERT_RESERVATION", false, (int) (System.currentTimeMillis() - startTime),
                        "SEAT_006", "Reservation is expired or released");
                throw new SeatReservationException("SEAT_006", "Reservation is expired or released", HttpStatus.BAD_REQUEST);
            }

            reservation.setStatus(SeatReservationStatus.BOOKED);
            reservation.setBookingId(request.getBookingId());
            seatReservationRepository.save(reservation);

            seatsByShowtime.computeIfAbsent(reservation.getShowtimeId(), k -> new ArrayList<>()).add(reservation.getSeatId());

            recordAuditLog("SYSTEM", "CONVERT_RESERVATION", "status", "HELD", "BOOKED");
            recordOutboxEvent("SeatReservation", reservation.getId(), "SEAT_CONVERTED", reservation);
        }

        for (Map.Entry<Long, List<Long>> entry : seatsByShowtime.entrySet()) {
            redisLockService.releaseLocks(entry.getKey(), entry.getValue(), "*");
        }

        recordOperationLog(request.getBookingId(), "CONVERT_RESERVATION", true, (int) (System.currentTimeMillis() - startTime), null, null);
    }

    @Override
    @Transactional
    public void expireReservations(List<Long> reservationIds) {
        long startTime = System.currentTimeMillis();

        if (reservationIds == null || reservationIds.isEmpty()) {
            return;
        }

        List<SeatReservation> reservations = seatReservationRepository.findAllByIdIn(reservationIds);
        Map<Long, List<Long>> seatsByShowtime = new java.util.HashMap<>();

        for (SeatReservation reservation : reservations) {
            if (reservation.getStatus() == SeatReservationStatus.HELD) {
                reservation.setStatus(SeatReservationStatus.EXPIRED);
                reservation.setExpiredReason("Scheduler expiration");
                seatReservationRepository.save(reservation);

                seatsByShowtime.computeIfAbsent(reservation.getShowtimeId(), k -> new ArrayList<>()).add(reservation.getSeatId());

                recordAuditLog("SCHEDULER", "EXPIRE_RESERVATION", "status", "HELD", "EXPIRED");
                recordOutboxEvent("SeatReservation", reservation.getId(), "SEAT_RESERVATION_EXPIRED", reservation);
            }
        }

        for (Map.Entry<Long, List<Long>> entry : seatsByShowtime.entrySet()) {
            redisLockService.releaseLocks(entry.getKey(), entry.getValue(), "*");
        }

        recordOperationLog(null, "EXPIRE_RESERVATIONS", true, (int) (System.currentTimeMillis() - startTime), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatReservationResponse findReservationByPublicId(String publicId, Long currentUserId, boolean isAdmin) {
        SeatReservation reservation = seatReservationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new SeatReservationException("SEAT_005", "Reservation not found", HttpStatus.NOT_FOUND));

        if (!isAdmin && !reservation.getUserId().equals(currentUserId)) {
            throw new SeatReservationException("SEAT_008", "Access denied to reservation", HttpStatus.FORBIDDEN);
        }

        return seatReservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SeatReservationResponse> findReservationsByUser(
            Long userId, SeatReservationStatus status, Long showtimeId, Pageable pageable) {

        Page<SeatReservation> page;
        if (status != null && showtimeId != null) {
            page = seatReservationRepository.findAllByUserIdAndShowtimeIdAndStatus(userId, showtimeId, status, pageable);
        } else if (status != null) {
            page = seatReservationRepository.findAllByUserIdAndStatus(userId, status, pageable);
        } else if (showtimeId != null) {
            page = seatReservationRepository.findAllByUserIdAndShowtimeId(userId, showtimeId, pageable);
        } else {
            page = seatReservationRepository.findAllByUserId(userId, pageable);
        }

        return page.map(seatReservationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatAvailabilityResponse checkAvailability(Long showtimeId, List<Long> seatIds) {
        if (showtimeId == null || seatIds == null || seatIds.isEmpty()) {
            return new SeatAvailabilityResponse(true, List.of());
        }

        Instant now = Instant.now();
        List<SeatReservation> activeReservations = seatReservationRepository.findActiveReservations(showtimeId, seatIds, now);
        List<Long> soldSeatIds = seatReservationRepository.findSoldSeatIdsFromBookings(showtimeId, seatIds);

        Set<Long> unavailableSet = new HashSet<>();
        for (SeatReservation res : activeReservations) {
            unavailableSet.add(res.getSeatId());
        }
        unavailableSet.addAll(soldSeatIds);

        List<Long> unavailableList = new ArrayList<>(unavailableSet);
        return new SeatAvailabilityResponse(unavailableList.isEmpty(), unavailableList);
    }

    @Override
    @Transactional(readOnly = true)
    public com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse getOccupiedSeatsByShowtime(String showtimeIdentifier) {
        if (showtimeIdentifier == null || showtimeIdentifier.isBlank()) {
            throw new SeatReservationException("SEAT_002", "Invalid showtime identifier", HttpStatus.BAD_REQUEST);
        }

        Long showtimeId = null;
        try {
            showtimeId = Long.parseLong(showtimeIdentifier);
        } catch (NumberFormatException e) {
            ShowtimeSeatLayoutResponse layout = movieServiceClient.getShowtimeSeatLayoutByPublicId(showtimeIdentifier);
            if (layout != null && layout.getShowtimeId() != null) {
                showtimeId = layout.getShowtimeId();
            }
        }

        if (showtimeId == null) {
            throw new SeatReservationException("SEAT_002", "Showtime not found for identifier: " + showtimeIdentifier, HttpStatus.NOT_FOUND);
        }

        Instant now = Instant.now();
        List<SeatReservation> activeReservations = seatReservationRepository.findAllActiveReservationsByShowtimeId(showtimeId, now);
        List<Long> soldSeatIds = seatReservationRepository.findSoldSeatIdsFromBookingsByShowtimeId(showtimeId);

        Map<Long, com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse.OccupiedSeatDto> occupiedMap = new java.util.HashMap<>();

        // 1. Add active reservations (HELD or BOOKED)
        for (SeatReservation res : activeReservations) {
            String statusStr = res.getStatus() != null ? res.getStatus().name() : "HELD";
            occupiedMap.put(res.getSeatId(), new com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse.OccupiedSeatDto(
                    res.getSeatId(),
                    res.getSeatLabel(),
                    statusStr,
                    res.getExpiresAt()
            ));
        }

        // 2. Add sold seats from bookings (status BOOKED)
        for (Long soldSeatId : soldSeatIds) {
            if (!occupiedMap.containsKey(soldSeatId)) {
                occupiedMap.put(soldSeatId, new com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse.OccupiedSeatDto(
                        soldSeatId,
                        "SEAT-" + soldSeatId,
                        "BOOKED",
                        null
                ));
            }
        }

        List<com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse.OccupiedSeatDto> occupiedList = new ArrayList<>(occupiedMap.values());
        return new com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse(showtimeIdentifier, occupiedList);
    }

    @Override
    @Transactional
    public com.lorafilm.booking.reservation.dto.ExtendReservationResponse extendReservation(String publicId, Long userId) {
        long startTime = System.currentTimeMillis();

        if (userId == null) {
            throw new SeatReservationException("SEAT_008", "User must be authenticated", HttpStatus.UNAUTHORIZED);
        }
        if (publicId == null || publicId.isBlank()) {
            throw new SeatReservationException("SEAT_005", "Reservation public ID is required", HttpStatus.BAD_REQUEST);
        }

        SeatReservation reservation = seatReservationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new SeatReservationException("SEAT_005", "Reservation not found", HttpStatus.NOT_FOUND));

        if (!reservation.getUserId().equals(userId)) {
            throw new SeatReservationException("SEAT_008", "Reservation does not belong to user", HttpStatus.FORBIDDEN);
        }
        if (reservation.getStatus() != SeatReservationStatus.HELD) {
            throw new SeatReservationException("SEAT_006", "Only active HELD reservations can be extended", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        if (reservation.getExpiresAt().isBefore(now)) {
            throw new SeatReservationException("SEAT_006", "Reservation is already expired", HttpStatus.BAD_REQUEST);
        }

        // Extension duration: 180 seconds (+3 minutes)
        long extensionSeconds = 180L;
        // Safety cap: max total reservation time 900 seconds (15 minutes) from reservedAt
        long maxTotalSeconds = 900L;
        Instant maxExpiresAt = reservation.getReservedAt().plusSeconds(maxTotalSeconds);

        Instant newExpiresAt = reservation.getExpiresAt().plusSeconds(extensionSeconds);
        if (newExpiresAt.isAfter(maxExpiresAt)) {
            newExpiresAt = maxExpiresAt;
        }

        if (newExpiresAt.isBefore(now) || newExpiresAt.equals(reservation.getExpiresAt())) {
            throw new SeatReservationException("SEAT_EXTEND_LIMIT", "Maximum reservation extension time reached", HttpStatus.BAD_REQUEST);
        }

        reservation.setExpiresAt(newExpiresAt);
        seatReservationRepository.save(reservation);

        // Refresh Redis lock for key
        long newTtlSeconds = newExpiresAt.getEpochSecond() - now.getEpochSecond();
        if (newTtlSeconds > 0) {
            String redisLockKey = "seat-lock:" + reservation.getShowtimeId() + ":" + reservation.getSeatId();
            redisLockService.acquireSingleLock(redisLockKey, reservation.getPublicId(), newTtlSeconds);
        }

        recordAuditLog(userId.toString(), "EXTEND_RESERVATION", "expiresAt", reservation.getExpiresAt().toString(), newExpiresAt.toString());
        recordOperationLog(null, "EXTEND_RESERVATION", true, (int) (System.currentTimeMillis() - startTime), null, null);

        return new com.lorafilm.booking.reservation.dto.ExtendReservationResponse(
                reservation.getPublicId(),
                reservation.getReservationCode(),
                newExpiresAt,
                extensionSeconds
        );
    }

    private void recordAuditLog(String actor, String action, String fieldName, String oldValue, String newValue) {
        try {
            BookingAuditLog audit = new BookingAuditLog();
            audit.setPublicId(UUID.randomUUID().toString());
            audit.setActor(actor);
            audit.setAction(action);
            audit.setFieldName(fieldName);
            audit.setOldValue(oldValue);
            audit.setNewValue(newValue);
            auditLogRepository.save(audit);
        } catch (Exception ex) {
            log.error("Failed to write audit log for action {}: ", action, ex);
        }
    }

    private void recordOperationLog(Long bookingId, String operationType, boolean success, int executionTimeMs, String errorCode, String errorMessage) {
        try {
            BookingOperationLog opLog = new BookingOperationLog();
            opLog.setPublicId(UUID.randomUUID().toString());
            opLog.setBookingId(bookingId);
            opLog.setOperationType(operationType);
            opLog.setActor("USER");
            opLog.setSuccess(success);
            opLog.setExecutionTimeMs((long) executionTimeMs);
            opLog.setErrorCode(errorCode);
            opLog.setErrorMessage(errorMessage);
            operationLogRepository.save(opLog);
        } catch (Exception ex) {
            log.error("Failed to write operation log for {}: ", operationType, ex);
        }
    }

    private void recordOutboxEvent(String aggregateType, Long aggregateId, String eventType, Object payload) {
        try {
            BookingOutboxEvent event = new BookingOutboxEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            event.setEventVersion(1);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus(OutboxStatus.PENDING);
            outboxEventRepository.save(event);
        } catch (Exception ex) {
            log.error("Failed to insert outbox event {}: ", eventType, ex);
            throw new SeatReservationException("INTERNAL_SERVER_ERROR", "Failed to record outbox event", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
    }
}

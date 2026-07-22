package com.lorafilm.booking.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.common.exception.SeatReservationException;
import com.lorafilm.booking.infrastructure.entity.BookingIdempotencyKey;
import com.lorafilm.booking.infrastructure.enums.IdempotencyStatus;
import com.lorafilm.booking.infrastructure.service.IdempotencyService;
import com.lorafilm.booking.reservation.dto.HoldSeatRequest;
import com.lorafilm.booking.reservation.dto.HoldSeatResponse;
import com.lorafilm.booking.reservation.dto.ReleaseSeatRequest;
import com.lorafilm.booking.reservation.dto.SeatReservationResponse;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import com.lorafilm.booking.security.service.SecurityContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/seat-reservations")
public class SeatReservationController {

    private static final Logger log = LoggerFactory.getLogger(SeatReservationController.class);

    private final SeatReservationService seatReservationService;
    private final SecurityContextService securityContextService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public SeatReservationController(
            SeatReservationService seatReservationService,
            SecurityContextService securityContextService,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.seatReservationService = seatReservationService;
        this.securityContextService = securityContextService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<HoldSeatResponse> holdSeats(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            HttpServletRequest httpRequest,
            @Valid @RequestBody HoldSeatRequest request) {

        String key = idempotencyKeyHeader;
        if (key == null || key.isBlank()) {
            key = httpRequest.getHeader("X-Idempotency-Key");
        }
        Long userId = securityContextService.getCurrentUserId();

        if (key != null && !key.isBlank()) {
            Optional<BookingIdempotencyKey> existingKey = idempotencyService.checkKey(key);
            if (existingKey.isPresent()) {
                BookingIdempotencyKey keyRecord = existingKey.get();
                if (keyRecord.getStatus() == IdempotencyStatus.COMPLETED) {
                    try {
                        HoldSeatResponse cachedResponse = objectMapper.readValue(keyRecord.getResponseBody(), HoldSeatResponse.class);
                        int status = keyRecord.getResponseStatus() != null ? keyRecord.getResponseStatus() : 201;
                        log.info("Returning cached idempotency response for key: {}", key);
                        return ResponseEntity.status(status).body(cachedResponse);
                    } catch (Exception e) {
                        log.error("Failed to parse cached idempotency response: ", e);
                    }
                } else if (keyRecord.getStatus() == IdempotencyStatus.PROCESSING) {
                    throw new SeatReservationException("IDEMPOTENCY_CONFLICT", "Request with this Idempotency-Key is currently being processed", HttpStatus.CONFLICT);
                }
            }
            idempotencyService.startProcessing(key, userId, "/api/seat-reservations", "POST", request);
        }

        try {
            HoldSeatResponse response = seatReservationService.holdSeats(userId, request);
            if (key != null && !key.isBlank()) {
                idempotencyService.completeProcessing(key, HttpStatus.CREATED.value(), response);
            }
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception ex) {
            if (key != null && !key.isBlank()) {
                idempotencyService.failProcessing(key);
            }
            throw ex;
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> releaseSeats(@Valid @RequestBody ReleaseSeatRequest request) {
        Long userId = securityContextService.getCurrentUserId();
        seatReservationService.releaseSeats(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<SeatReservationResponse>> getMyReservations(
            @RequestParam(required = false) SeatReservationStatus status,
            @RequestParam(required = false) Long showtimeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = securityContextService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SeatReservationResponse> responses = seatReservationService.findReservationsByUser(userId, status, showtimeId, pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<SeatReservationResponse> getReservationDetail(@PathVariable String publicId) {
        Long currentUserId = securityContextService.getCurrentUserId();
        boolean isAdmin = securityContextService.isAdmin();
        SeatReservationResponse response = seatReservationService.findReservationByPublicId(publicId, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/showtime/{showtimeId}/occupied-seats")
    public ResponseEntity<com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse> getOccupiedSeatsByShowtime(
            @PathVariable String showtimeId) {
        com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse response = seatReservationService.getOccupiedSeatsByShowtime(showtimeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{publicId}/extend")
    public ResponseEntity<com.lorafilm.booking.reservation.dto.ExtendReservationResponse> extendReservation(
            @PathVariable String publicId) {
        Long currentUserId = securityContextService.getCurrentUserId();
        com.lorafilm.booking.reservation.dto.ExtendReservationResponse response = seatReservationService.extendReservation(publicId, currentUserId);
        return ResponseEntity.ok(response);
    }
}

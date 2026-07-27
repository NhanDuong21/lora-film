package com.lorafilm.booking.reservation.controller;

import com.lorafilm.booking.reservation.dto.ConvertReservationRequest;
import com.lorafilm.booking.reservation.dto.ReleaseSeatRequest;
import com.lorafilm.booking.reservation.dto.SeatAvailabilityResponse;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.lorafilm.booking.common.exception.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/seat-reservations")
public class InternalSeatReservationController {

    private final SeatReservationService seatReservationService;

    public InternalSeatReservationController(SeatReservationService seatReservationService) {
        this.seatReservationService = seatReservationService;
    }

    @PostMapping("/convert")
    public ResponseEntity<Void> convertReservation(@Valid @RequestBody ConvertReservationRequest request) {
        throw new BusinessException("ATOMIC_BOOKING_CREATION_REQUIRED",
                "Reservations are created and linked by the canonical Booking coordinator",
                HttpStatus.GONE);
    }

    @PostMapping("/release")
    public ResponseEntity<Void> releaseReservation(@Valid @RequestBody ReleaseSeatRequest request) {
        throw new BusinessException("BOOKING_LIFECYCLE_REQUIRED",
                "Linked reservation release is owned by Booking lifecycle", HttpStatus.GONE);
    }

    @PostMapping("/expire")
    public ResponseEntity<Void> expireReservation(@RequestBody List<Long> reservationIds) {
        throw new BusinessException("BOOKING_LIFECYCLE_REQUIRED",
                "Linked reservation expiry is owned by Booking lifecycle", HttpStatus.GONE);
    }

    @GetMapping("/availability")
    public ResponseEntity<SeatAvailabilityResponse> checkAvailability(
            @RequestParam Long showtimeId,
            @RequestParam List<Long> seatIds) {
        SeatAvailabilityResponse response = seatReservationService.checkAvailability(showtimeId, seatIds);
        return ResponseEntity.ok(response);
    }
}

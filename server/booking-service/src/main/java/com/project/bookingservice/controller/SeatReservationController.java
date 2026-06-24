package com.project.bookingservice.controller;

import com.project.bookingservice.dto.reservation.CreateReservationRequest;
import com.project.bookingservice.dto.reservation.ReservationResponse;
import com.project.bookingservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/bookings/seat-reservations")
@Tag(name = "Seat Reservations", description = "Endpoints for managing seat reservations")
public class SeatReservationController {

    private final ReservationService reservationService;

    public SeatReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create a new seat reservation", description = "Reserves one or more seats for a showtime atomically. Requires Idempotency-Key header.")
    public ResponseEntity<List<ReservationResponse>> createReservation(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request) {

        List<ReservationResponse> responses = reservationService.createReservation(request, idempotencyKey);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Get a seat reservation", description = "Retrieve details of a specific seat reservation by ID. User must own the reservation.")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long reservationId) {
        ReservationResponse response = reservationService.getReservation(reservationId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reservationId}")
    @Operation(summary = "Release a seat reservation", description = "Release a held seat reservation. Idempotent operation. User must own the reservation.")
    public ResponseEntity<Void> releaseReservation(@PathVariable Long reservationId) {
        reservationService.releaseReservation(reservationId);
        return ResponseEntity.noContent().build();
    }
}
